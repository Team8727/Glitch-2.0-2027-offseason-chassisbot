// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.Subsystems;

import com.ctre.phoenix6.StatusCode;
import com.ctre.phoenix6.configs.CANdleConfiguration;
import com.ctre.phoenix6.controls.ControlRequest;
import com.ctre.phoenix6.controls.EmptyAnimation;
import com.ctre.phoenix6.controls.SolidColor;
import com.ctre.phoenix6.hardware.CANdle;
import com.ctre.phoenix6.signals.Enable5VRailValue;
import com.ctre.phoenix6.signals.RGBWColor;
import com.ctre.phoenix6.signals.StripTypeValue;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

/**
 * Abstract base class for a Phoenix 6 CANdle-driven LED strip.
 *
 * <p>The CANdle renders animations on the device itself, so this class manages a
 * single animation over the whole strip. Subclasses pick the animation per robot
 * mode (e.g. a flowing team-color pattern while disabled or enabled) and can
 * temporarily override it for a duration before reverting to the base animation.
 */
public abstract class CandleLEDs extends SubsystemBase {

  private final CANdle candle;
  private final int ledCount;
  private ControlRequest baseRequest;
  private ControlRequest activeRequest;
  private double overrideEndsAt = Double.POSITIVE_INFINITY;
  private boolean enabled = true;
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
    this.candle = new CANdle(candleId);
    this.ledCount = ledCount;

    CANdleConfiguration config = new CANdleConfiguration();
    config.LED.StripType = stripType;
    config.LED.BrightnessScalar = brightnessScalar;
    // CANdle VBUS powers the strip; without this nothing lights up.
    config.CANdleFeatures.Enable5VRail = Enable5VRailValue.Enabled;
    candle.getConfigurator().apply(config);

    baseRequest = new SolidColor(0, Math.max(0, ledCount - 1)).withColor(new RGBWColor(0, 0, 0));
    activeRequest = baseRequest;
  }

  /** Returns the number of LEDs wired to the CANdle. */
  protected int getLedCount() {
    return ledCount;
  }

  /**
   * Sets the animation the strip returns to once a timed override has completed.
   *
   * @param request The base animation to display.
   */
  protected void setBaseAnimation(ControlRequest request) {
    baseRequest = request;
    setAnimation(request);
  }

  /** Returns the animation the strip reverts to once timed overrides complete. */
  protected ControlRequest getBaseAnimation() {
    return baseRequest;
  }

  /**
   * Sets the strip animation indefinitely, overriding the base until changed.
   *
   * @param request The animation to display.
   */
  protected void setAnimation(ControlRequest request) {
    activeRequest = request;
    overrideEndsAt = Double.POSITIVE_INFINITY;
    send();
  }

  /**
   * Sets the strip animation for a finite duration, after which the strip reverts
   * to its base animation.
   *
   * @param request The animation to display.
   * @param durationSeconds How long to display it before reverting to the base.
   */
  protected void setAnimation(ControlRequest request, double durationSeconds) {
    activeRequest = request;
    overrideEndsAt = Timer.getFPGATimestamp() + durationSeconds;
    send();
  }

  /** Clears any override and returns the strip to the base animation. */
  protected void clearAnimationOverride() {
    setAnimation(baseRequest);
  }

  /**
   * Turns the strip rendering on or off. Disabling also blanks the strip, which is
   * useful for an emergency off hook.
   *
   * @param enabled Whether the strip should keep rendering.
   */
  public void setEnabled(boolean enabled) {
    if (this.enabled == enabled) {
      return;
    }
    this.enabled = enabled;
    if (enabled) {
      send();
    } else {
      candle.setControl(new EmptyAnimation(0));
    }
  }

  /** Returns whether the strip is currently rendering animations. */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Reverts timed overrides once they expire. Runs automatically via the command
   * scheduler since this class extends {@link SubsystemBase}.
   */
  @Override
  public void periodic() {
    if (!enabled) {
      return;
    }
    if (overrideEndsAt != Double.POSITIVE_INFINITY && Timer.getFPGATimestamp() >= overrideEndsAt) {
      setAnimation(baseRequest);
    }
  }

  private void send() {
    StatusCode status = candle.setControl(activeRequest);
    if (status != StatusCode.OK && !candleUnhealthyLogged) {
      candleUnhealthyLogged = true;
      DriverStation.reportError(
          "CANdle is not responding over CAN. Check the device ID and wiring.", false);
    } else if (status == StatusCode.OK) {
      candleUnhealthyLogged = false;
    }
  }
}