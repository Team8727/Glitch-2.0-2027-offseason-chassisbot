package frc.robot.Subsystems;

import java.util.Random;

import com.ctre.phoenix6.controls.SolidColor;
import com.ctre.phoenix6.signals.AnimationDirectionValue;
import com.ctre.phoenix6.signals.RGBWColor;

import edu.wpi.first.wpilibj.Timer;

/**
 * Software-driven fire animation supporting selectable color palettes.
 *
 * <p>Port of Lynk 9496's {@code CustomFireAnimation} (Fire2012 / Mark Kriegsman)
 * tuned to visually match Phoenix 6's built-in {@code FireAnimation}, but with a
 * configurable palette so the same flame effect can render as team blue ("icy
 * fire") or team green.
 *
 * <p>Keeps CAN and CPU use low:
 * <ul>
 *   <li>Heat is quantized to a small number of levels; adjacent LEDs at the same
 *       level are coalesced into a single {@link SolidColor} CAN frame.</li>
 *   <li>Only changed ranges are re-sent, and {@code UpdateFreqHz=0} makes each
 *       {@code setControl} a single one-shot CAN frame.</li>
 *   <li>The simulation advances only when the frame timer elapses, so calling
 *       {@link #update} every robot tick is cheap.</li>
 * </ul>
 *
 * <p>MIT license, Copyright (c) 2026 LYNK 9496.
 */
public class CustomFireAnimation {
  public enum Palette {
    /** Black -> Red -> Yellow -> White (matches Phoenix 6 FireAnimation). */
    RED,
    /** Black -> Blue -> Cyan -> White ("icy fire"). */
    BLUE,
    /** Black -> Green -> Yellow-green -> White (team Glitch green). */
    GREEN,
  }

  /** Number of quantized intensity levels; fewer = fewer CAN frames per update. */
  private static final int QUANT_LEVELS = 8;

  private final int startIdx;
  private final int numLEDs;

  private final int[] heat;
  private final int[] lastLevel;

  private final RGBWColor[] palettesByLevel;

  private final Timer frameTimer = new Timer();
  private final Random random = new Random();

  private double frameRate = 60.0;
  private double brightness = 1.0;
  private double sparking = 0.5;
  private double cooling = 0.5;
  private AnimationDirectionValue direction = AnimationDirectionValue.Forward;

  private Palette palette = Palette.BLUE;

  private boolean palettesDirty = true;
  private boolean forceFullSend = true;

  /**
   * Construct a CustomFireAnimation over an inclusive LED index range.
   *
   * @param ledStartIndex first LED index
   * @param ledEndIndex   last LED index, inclusive
   */
  public CustomFireAnimation(int ledStartIndex, int ledEndIndex) {
    this.startIdx = ledStartIndex;
    this.numLEDs = ledEndIndex - ledStartIndex + 1;
    this.heat = new int[numLEDs];
    this.lastLevel = new int[numLEDs];
    this.palettesByLevel = new RGBWColor[QUANT_LEVELS];
    for (int i = 0; i < numLEDs; i++) {
      lastLevel[i] = -1;
    }
    frameTimer.start();
  }

  public CustomFireAnimation withFrameRate(double frameRate) {
    this.frameRate = frameRate;
    return this;
  }

  public CustomFireAnimation withBrightness(double brightness) {
    if (this.brightness != brightness) {
      this.brightness = brightness;
      this.palettesDirty = true;
      this.forceFullSend = true;
    }
    return this;
  }

  public CustomFireAnimation withSparking(double sparking) {
    this.sparking = sparking;
    return this;
  }

  public CustomFireAnimation withCooling(double cooling) {
    this.cooling = cooling;
    return this;
  }

  public CustomFireAnimation withDirection(AnimationDirectionValue direction) {
    if (this.direction != direction) {
      this.direction = direction;
      this.forceFullSend = true;
    }
    return this;
  }

  public CustomFireAnimation withPalette(Palette palette) {
    if (this.palette != palette) {
      this.palette = palette;
      this.palettesDirty = true;
      this.forceFullSend = true;
    }
    return this;
  }

  public Palette getPalette() {
    return palette;
  }

  /** Reset the simulation; next {@link #update} renders a full fresh frame. */
  public void reset() {
    for (int i = 0; i < numLEDs; i++) {
      heat[i] = 0;
      lastLevel[i] = -1;
    }
    forceFullSend = true;
    frameTimer.restart();
  }

  /**
   * Advance the animation by one frame if the frame-rate interval has elapsed.
   *
   * <p>Safe to call every robot tick; internally throttled to {@link #withFrameRate}.
   *
   * @param leds LED group to send updates to
   * @return true if a new frame was rendered on this call
   */
  public boolean update(CANdleGroup leds) {
    if (!frameTimer.hasElapsed(1.0 / frameRate)) {
      return false;
    }
    frameTimer.restart();

    if (palettesDirty) {
      rebuildPalette();
    }

    simulateStep();
    renderRunLengthEncoded(leds);
    return true;
  }

  private void simulateStep() {
    // Cool down every cell a little. Maps cooling 0.0-1.0 onto Fire2012's
    // 0-100 cooling scale via the original `((COOLING * 10) / N) + 2` formula.
    int coolMax = Math.max(2, (int) Math.round(cooling * 100.0 * 10.0 / numLEDs) + 2);
    for (int i = 0; i < numLEDs; i++) {
      heat[i] = Math.max(0, heat[i] - random.nextInt(coolMax));
    }

    // Heat drifts upward and diffuses (Fire2012's exact recurrence).
    for (int k = numLEDs - 1; k >= 2; k--) {
      heat[k] = (heat[k - 1] + heat[k - 2] + heat[k - 2]) / 3;
    }

    // Randomly ignite new sparks near the bottom.
    if (random.nextDouble() < sparking) {
      int y = random.nextInt(Math.min(7, numLEDs));
      heat[y] = Math.min(255, heat[y] + 160 + random.nextInt(96));
    }
  }

  private void renderRunLengthEncoded(CANdleGroup leds) {
    boolean reversed = direction == AnimationDirectionValue.Backward;

    int runStart = 0;
    int runLevel = quantize(heat[reversed ? numLEDs - 1 : 0]);

    // Sentinel level at i==numLEDs forces the last run to flush.
    for (int i = 1; i <= numLEDs; i++) {
      int level = (i == numLEDs) ? -1 : quantize(heat[reversed ? numLEDs - 1 - i : i]);
      if (level != runLevel) {
        flushRun(leds, runStart, i - 1, runLevel);
        runStart = i;
        runLevel = level;
      }
    }
    forceFullSend = false;
  }

  private void flushRun(CANdleGroup leds, int physStart, int physEnd, int level) {
    boolean changed = forceFullSend;
    if (!changed) {
      for (int j = physStart; j <= physEnd; j++) {
        if (lastLevel[j] != level) {
          changed = true;
          break;
        }
      }
    }
    if (!changed) {
      return;
    }

    // Fresh SolidColor per range so we never hand a mutated request back to
    // Phoenix 6 after setControl has consumed it. UpdateFreqHz=0 = one-shot frame.
    SolidColor req =
        new SolidColor(startIdx + physStart, startIdx + physEnd)
            .withColor(palettesByLevel[level])
            .withUpdateFreqHz(0);
    leds.setControl(req);

    for (int j = physStart; j <= physEnd; j++) {
      lastLevel[j] = level;
    }
  }

  private int quantize(int heatVal) {
    if (heatVal <= 0) {
      return 0;
    }
    if (heatVal >= 255) {
      return QUANT_LEVELS - 1;
    }
    return (heatVal * QUANT_LEVELS) >> 8;
  }

  private void rebuildPalette() {
    for (int i = 0; i < QUANT_LEVELS; i++) {
      int heatVal = (i * 255) / (QUANT_LEVELS - 1);
      palettesByLevel[i] = heatToColor(heatVal, palette, brightness);
    }
    palettesDirty = false;
  }

  private static RGBWColor heatToColor(int heat, Palette palette, double brightness) {
    // Port of Mark Kriegsman's HeatColor() from FastLED: maps 0-255 heat through
    // a three-band ramp (black -> primary -> primary+secondary -> white).
    int t192 = (heat * 191) >> 8;
    int heatramp = (t192 & 0x3F) << 2;

    int r, g, b;
    if ((t192 & 0x80) != 0) {
      r = 255;
      g = 255;
      b = heatramp;
    } else if ((t192 & 0x40) != 0) {
      r = 255;
      g = heatramp;
      b = 0;
    } else {
      r = heatramp;
      g = 0;
      b = 0;
    }

    if (palette == Palette.BLUE) {
      int tmp = r;
      r = b;
      b = tmp;
    } else if (palette == Palette.GREEN) {
      // Red ramp -> green ramp: black -> green -> green+yellow -> white.
      int tmp = r;
      r = b;
      b = 0;
      g = tmp;
    }

    r = clamp((int) Math.round(r * brightness));
    g = clamp((int) Math.round(g * brightness));
    b = clamp((int) Math.round(b * brightness));

    return new RGBWColor(r, g, b, 0);
  }

  private static int clamp(int v) {
    if (v < 0) {
      return 0;
    }
    if (v > 255) {
      return 255;
    }
    return v;
  }
}
