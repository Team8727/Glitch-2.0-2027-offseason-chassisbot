// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.Subsystems;

import com.ctre.phoenix6.StatusCode;
import com.ctre.phoenix6.configs.CandleConfiguration;
import com.ctre.phoenix6.configs.LedStripType;
import com.ctre.phoenix6.led.Candle;
import edu.wpi.first.wpilibj.AddressableLEDBuffer;
import edu.wpi.first.wpilibj.AddressableLEDBufferView;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.LEDPattern;
import edu.wpi.first.wpilibj.RobotController;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

import static edu.wpi.first.units.Units.Microseconds;
import static edu.wpi.first.units.Units.Seconds;

/**
 * Abstract base class for a CTRE CANdle-driven LED strip.
 *
 * <p>This class replaces the PWM {@code AddressableLED} layer of GlitchLib's
 * {@code AbstractLEDS} with a Phoenix 6 {@link Candle}, while keeping the same
 * per-section pattern model: each section has a base pattern that it returns to
 * after a timed pattern finishes, so WPILib {@link LEDPattern}s (including the
 * GlitchLib fire patterns) compose exactly like they do on the PWM strip.
 *
 * <p>Because the CANdle cannot consume an {@code AddressableLEDBuffer} directly,
 * patterns are rendered into an on-roboRIO buffer and then streamed to the CANdle
 * pixel by pixel with {@link Candle#setLEDs(int, int, int, int, int)}. To comply
 * with the team's anti-flicker / epilepsy guideline, the strip is only refreshed
 * at most once every {@link #ledRefreshPeriodSeconds} seconds, matching the cadence
 * of GlitchLib's {@code AbstractLEDS}.
 */
public abstract class CandleLEDs extends SubsystemBase {

  /** Duration sentinel meaning "run forever". */
  public static final double infiniteDurationSeconds = -1.0;

  /** Maximum strip refresh rate in seconds (eye-safety throttle). */
  public static final double ledRefreshPeriodSeconds = 0.2;

  private final Candle candle;
  private final AddressableLEDBuffer stripBuffer;

  /** The single continuous section covering the whole strip. */
  protected final Section strip;

  private boolean enabled = true;
  private double lastUpdateTimeMicroseconds;
  private double lastLedWriteTimeMicroseconds;
  private double deltaTimeSeconds;
  private boolean candleUnhealthyLogged = false;

  /**
   * Creates a new CANdle-driven LED strip.
   *
   * @param candleId The CAN device ID of the CANdle.
   * @param ledCount The total number of LEDs in the strip.
   * @param brightnessScalar Global brightness from 0.0 to 1.0 applied by the CANdle.
   * @param stripType The RGB channel order of the physical strip.
   */
  protected CandleLEDs(int candleId, int ledCount, double brightnessScalar, LedStripType stripType) {
    candle = new Candle(candleId);

    CandleConfiguration config = new CandleConfiguration();
    config.brightnessScalar = brightnessScalar;
    config.v5Enabled = true; // CANdle VBUS powers the strip; without this nothing lights up.
    config.stripType = stripType;
    // Note: field names / return types in CandleConfiguration depend on the vendored
    // Phoenix 6 version (26.1.1 in the 2026 repo); rename if the build disagrees.
    candle.getConfigurator().apply(config);

    stripBuffer = new AddressableLEDBuffer(ledCount);
    strip = new Section(0, ledCount - 1);
  }

  /**
   * Represents a section of the LED strip with a base pattern and an optional
   * finite-duration pattern that reverts to the base when it completes.
   */
  public class Section {
    private final AddressableLEDBufferView bufferView;
    private LEDPattern pattern = LEDPattern.kOff;
    private LEDPattern basePattern = LEDPattern.kOff;
    private double durationSeconds = infiniteDurationSeconds;
    private double elapsedSeconds = 0.0;

    private Section(int startIndex, int endIndex) {
      bufferView = stripBuffer.createView(startIndex, endIndex);
    }

    /**
     * Sets the section's pattern for a finite duration, after which the section
     * returns to its base pattern.
     *
     * @param pattern The pattern to display.
     * @param durationSeconds How long to display it before reverting to the base.
     */
    public void setPattern(LEDPattern pattern, double durationSeconds) {
      if (pattern == null) {
        pattern = LEDPattern.kOff;
      }
      this.pattern = pattern;
      this.durationSeconds = durationSeconds;
      this.elapsedSeconds = 0.0;
    }

    /**
     * Sets the section's pattern indefinitely.
     *
     * @param pattern The pattern to display.
     */
    public void setPattern(LEDPattern pattern) {
      setPattern(pattern, infiniteDurationSeconds);
    }

    /**
     * Sets the pattern the section returns to once a timed pattern has completed.
     *
     * @param basePattern The fallback pattern.
     */
    public void setBase(LEDPattern basePattern) {
      if (basePattern == null) {
        basePattern = LEDPattern.kOff;
      }
      this.basePattern = basePattern;
    }

    /**
     * Advances the section by the given time, applying its active pattern to the
     * underlying buffer view. Called by {@link CandleLEDs#periodic()}.
     *
     * @param deltaTimeSeconds Time elapsed since the last update, in seconds.
     */
    void update(double deltaTimeSeconds) {
      if (durationSeconds != infiniteDurationSeconds) {
        elapsedSeconds += deltaTimeSeconds;
        if (elapsedSeconds >= durationSeconds) {
          setPattern(basePattern);
        }
      }

      pattern.applyTo(bufferView);
    }

    /** Returns the buffer view for this section. */
    public AddressableLEDBufferView getBufferView() {
      return bufferView;
    }

    /** Returns the number of LEDs in this section. */
    public int getLength() {
      return bufferView.getLength();
    }

    /** Returns the pattern currently applied to this section. */
    public LEDPattern getCurrentPattern() {
      return pattern;
    }

    /** Returns the pattern this section reverts to once timed patterns complete. */
    public LEDPattern getBasePattern() {
      return basePattern;
    }
  }

  /**
   * Turns the strip rendering on or off. Disabling also blanks the strip, which is
   * useful for an emergency off hook.
   *
   * @param enabled Whether the strip should keep rendering.
   */
  public void setEnabled(boolean enabled) {
    if (!enabled && this.enabled) {
      candle.setLEDs(0, stripBuffer.getLength(), 0, 0, 0);
    }
    this.enabled = enabled;
  }

  /** Returns whether the strip is currently rendering patterns. */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Updates all sections and streams the rendered buffer to the CANdle, throttled
   * to {@link #ledRefreshPeriodSeconds}. Runs automatically via the command
   * scheduler since this class extends {@link SubsystemBase}.
   */
  @Override
  public void periodic() {
    double now = RobotController.getTime();
    if (lastUpdateTimeMicroseconds == 0.0) {
      deltaTimeSeconds = 0.02;
    } else {
      deltaTimeSeconds = Microseconds.of(now - lastUpdateTimeMicroseconds).in(Seconds);
    }
    lastUpdateTimeMicroseconds = now;

    strip.update(deltaTimeSeconds);

    if (!enabled) {
      return;
    }

    if (now - lastLedWriteTimeMicroseconds < Seconds.of(ledRefreshPeriodSeconds).in(Microseconds)) {
      return;
    }
    lastLedWriteTimeMicroseconds = now;

    pushToCandle();
  }

  private void pushToCandle() {
    boolean healthy = true;
    for (int i = 0; i < stripBuffer.getLength(); i++) {
      if (candle.setLEDs(i, 1, stripBuffer.getRed(i), stripBuffer.getGreen(i), stripBuffer.getBlue(i))
          != StatusCode.OK) {
        healthy = false;
        break;
      }
    }

    if (!healthy) {
      if (!candleUnhealthyLogged) {
        candleUnhealthyLogged = true;
        DriverStation.reportError(
            "CANdle is not responding over CAN. Check the device ID and wiring.", false);
      }
      // Keep lastLedWriteTimeMicroseconds untouched so retries stay on the 0.2 s throttle
      // instead of bursting every robot period while the fault persists.
    } else {
      candleUnhealthyLogged = false;
    }
  }
}