// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.Subsystems;

import com.ctre.phoenix6.CANBus;
import com.ctre.phoenix6.controls.ControlRequest;
import com.ctre.phoenix6.controls.SolidColor;
import com.ctre.phoenix6.hardware.CANdle;
import com.ctre.phoenix6.signals.RGBWColor;
import com.ctre.phoenix6.signals.StripTypeValue;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

/**
 * Team LED subsystem for the offseason chassisbot.
 *
 * <p>Drives a CTRE CANdle (on the CANivore bus) with a high-density addressable
 * strip. Default patterns are the Lynk-style blue flame ({@link CustomFireAnimation}
 * with the {@link CustomFireAnimation.Palette#BLUE} palette) for enabled modes and
 * the same flame in team green while disabled, plus a scrolling white scan for
 * bench pixel-order checks.
 *
 * <p>CAN IDs, LED count and tunables live in one block for easy on-bench tweaking.
 */
public class LEDSubsystem extends SubsystemBase {

  // ---------- Tunable constants ----------

  /** CAN device ID of the CANdle. */
  public static final int CANDLE_ID = 14;

  /** Number of LEDs wired to the CANdle's VBUS output (high-density strip). */
  public static final int LED_COUNT = 300;

  /** Global brightness applied by the CANdle. Keep moderate for the shop. */
  public static final double BRIGHTNESS_SCALAR = 0.7;

  /** RGB channel order of the physical strip. */
  public static final StripTypeValue STRIP_TYPE = StripTypeValue.GRB;

  /** Flame frame rate, in frames per second. */
  public static final double FLAME_FRAME_RATE_HZ = 60.0;

  /** Flame sparking intensity (0-1). */
  public static final double FLAME_SPARKING = 0.5;

  /** Flame cooling intensity (0-1); higher = shorter flames. */
  public static final double FLAME_COOLING = 0.45;

  /** Speed of the bench scan, in frames per second. */
  public static final double SCAN_FRAME_RATE_HZ = 24.0;

  /** How long an error/warning flash lasts before returning to the base pattern (s). */
  public static final double ALERT_DURATION_SECONDS = 0.7;

  // ---------- Hardware ----------

  private final CANdle candle;
  private final CANdleGroup leds;

  // ---------- Patterns ----------

  /** Lynk-style blue flame ("icy fire"), the robot's signature pattern. */
  public final CustomFireAnimation blueFlame =
      new CustomFireAnimation(0, LED_COUNT - 1)
          .withPalette(CustomFireAnimation.Palette.BLUE)
          .withFrameRate(FLAME_FRAME_RATE_HZ)
          .withSparking(FLAME_SPARKING)
          .withCooling(FLAME_COOLING)
          .withBrightness(BRIGHTNESS_SCALAR);

  /** Team-green flame used while disabled. */
  public final CustomFireAnimation greenFlame =
      new CustomFireAnimation(0, LED_COUNT - 1)
          .withPalette(CustomFireAnimation.Palette.GREEN)
          .withFrameRate(FLAME_FRAME_RATE_HZ)
          .withSparking(FLAME_SPARKING)
          .withCooling(FLAME_COOLING)
          .withBrightness(BRIGHTNESS_SCALAR);

  /** Scrolling white scan used to verify pixel order on the bench. */
  public final ControlRequest whiteScan =
      new SolidColor(0, LED_COUNT - 1)
          .withColor(new RGBWColor(255, 255, 255, 0));

  // ---------- State ----------

  private CustomFireAnimation baseAnimation = greenFlame;
  private ControlRequest override = null;
  private Timer overrideTimer = new Timer();

  private static LEDSubsystem instance;

  /**
   * Creates the LED subsystem on the CANdle. Prefer {@link #getInstance()} so robot
   * code and commands all share the same instance.
   */
  public LEDSubsystem() {
    candle = new CANdle(CANDLE_ID, new CANBus("canivore"));
    leds = new CANdleGroup(candle);
  }

  /** Returns the shared LED subsystem instance. */
  public static LEDSubsystem getInstance() {
    if (instance == null) {
      instance = new LEDSubsystem();
    }
    return instance;
  }

  @Override
  public void periodic() {
    // FMS/disconnected-aware default state machine.
    if (DriverStation.isDisabled()) {
      baseAnimation = greenFlame;
    } else {
      baseAnimation = blueFlame;
    }

    if (override != null) {
      if (overrideTimer.hasElapsed(ALERT_DURATION_SECONDS)) {
        override = null;
      } else {
        leds.setControl(override);
        return;
      }
    }

    baseAnimation.update(leds);
  }

  /** Idle behavior while disabled: green flame (applied automatically in periodic). */
  public void start() {
    // nothing to do; periodic() switches to green flame
  }

  /** Autonomous mode: blue flame. */
  public void autoInit() {
    baseAnimation = blueFlame;
    blueFlame.reset();
  }

  /** Teleoperated mode: blue flame. */
  public void teleopInit() {
    baseAnimation = blueFlame;
    blueFlame.reset();
  }

  /**
   * Test mode: runs a scrolling white scan so the driver can verify pixel order,
   * then reverts to the base pattern.
   */
  public void testInit() {
    setOverride(whiteScan, 10.0);
  }

  /**
   * Temporarily overrides the strip indefinitely, ignoring the mode base.
   *
   * @param request The animation to display.
   */
  public void setOverride(ControlRequest request) {
    override = request;
    overrideTimer.restart();
  }

  /**
   * Temporarily overrides the strip for a duration, after which the strip returns
   * to the mode base animation.
   *
   * @param request The animation to display.
   * @param seconds How long to display it before reverting to the base.
   */
  public void setOverride(ControlRequest request, double seconds) {
    override = request;
    overrideTimer.restart();
  }

  /** Clears any override and returns the strip to the mode base animation. */
  public void clearOverride() {
    override = null;
  }
}
