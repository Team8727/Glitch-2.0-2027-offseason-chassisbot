// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.Subsystems;

import com.ctre.phoenix6.signals.StripTypeValue;
import edu.wpi.first.wpilibj.AddressableLEDBuffer;
import edu.wpi.first.wpilibj.AddressableLEDBufferView;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.LEDPattern;
import edu.wpi.first.wpilibj.RobotController;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

import static edu.wpi.first.units.Units.Microseconds;
import static edu.wpi.first.units.Units.Seconds;

/**
 * Abstract base class for a CTRE CANdle-driven LED strip.
 *
 * <p>This class replaces the PWM {@code AddressableLED} layer of GlitchLib's
 * {@code AbstractLEDS} with a Phoenix 6 CANdle, while keeping the same
 * per-section pattern model: each section has a base pattern that it returns to
 * after a timed pattern finishes, so WPILib {@link LEDPattern}s (including the
 * GlitchLib fire patterns) compose exactly like they do on the PWM strip.
 *
 * <p>Because the CANdle cannot consume an {@code AddressableLEDBuffer} directly,
 * patterns are rendered into an on-roboRIO buffer and then streamed to the CANdle
 * through a hardware adapter. The strip is only refreshed
 * at most once every {@link #ledRefreshPeriodSeconds} seconds, matching the cadence
 * of GlitchLib's {@code AbstractLEDS}. This limits CAN traffic; it is not an
 * assessment of visual or photosensitivity safety.
 */
public abstract class CandleLEDs extends SubsystemBase {

  /** Duration sentinel meaning "run forever". */
  public static final double infiniteDurationSeconds = -1.0;

  /** Minimum interval between strip refreshes in seconds. */
  public static final double ledRefreshPeriodSeconds = 0.2;

  private final LedOutput output;
  private final LongSupplier clockMicroseconds;
  private final Consumer<String> reportError;
  private final AddressableLEDBuffer stripBuffer;

  /** The single continuous section covering the whole strip. */
  protected final Section strip;

  private boolean enabled = true;
  private double lastUpdateTimeMicroseconds = Double.NaN;
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
  protected CandleLEDs(int candleId, int ledCount, double brightnessScalar, StripTypeValue stripType) {
    this(ledCount, new PhoenixLedOutput(candleId, brightnessScalar, stripType),
        RobotController::getTime, message -> DriverStation.reportError(message, false));
  }

  /** Hardware/time seam: tests exercise the same rendering and retry logic as the robot. */
  protected CandleLEDs(int ledCount, LedOutput output, LongSupplier clockMicroseconds,
      Consumer<String> reportError) {
    this.output = output;
    this.clockMicroseconds = clockMicroseconds;
    this.reportError = reportError;
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
      output.setLEDs(0, stripBuffer.getLength(), 0, 0, 0);
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
    double now = clockMicroseconds.getAsLong();
    if (Double.isNaN(lastUpdateTimeMicroseconds)) {
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
      if (!output.setLEDs(i, 1, stripBuffer.getRed(i), stripBuffer.getGreen(i), stripBuffer.getBlue(i))) {
        healthy = false;
        break;
      }
    }

    if (!healthy) {
      if (!candleUnhealthyLogged) {
        candleUnhealthyLogged = true;
        reportError.accept(
            "CANdle is not responding over CAN. Check the device ID and wiring.");
      }
      // Keep lastLedWriteTimeMicroseconds untouched so retries stay on the 0.2 s throttle
      // instead of bursting every robot period while the fault persists.
    } else {
      candleUnhealthyLogged = false;
    }
  }
}
