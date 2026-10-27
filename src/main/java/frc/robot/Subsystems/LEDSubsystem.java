// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.Subsystems;

import com.ctre.phoenix6.controls.ColorFlowAnimation;
import com.ctre.phoenix6.controls.ControlRequest;
import com.ctre.phoenix6.signals.AnimationDirectionValue;
import com.ctre.phoenix6.signals.RGBWColor;
import com.ctre.phoenix6.signals.StripTypeValue;

/**
 * Team LED subsystem for the offseason chassisbot.
 *
 * <p>Drives the CTRE CANdle with flowing team-color patterns built from the
 * CANdle's on-device animations. The strip is a single continuous segment of
 * {@value #LED_COUNT} LEDs, so all of the tunable values below live in one
 * block for easy on-bench adjustment.
 */
public class LEDSubsystem extends CandleLEDs {

  // ---------- Tunable constants ----------

  /** CAN device ID of the CANdle. */
  public static final int CANDLE_ID = 14;

  /** Number of LEDs wired to the CANdle's VBUS output. */
  public static final int LED_COUNT = 60;

  /** Global brightness applied by the CANdle. Keep moderate for the shop. */
  public static final double BRIGHTNESS_SCALAR = 0.7;

  /** RGB channel order of the physical strip. */
  public static final StripTypeValue STRIP_TYPE = StripTypeValue.RGB;

  /** Deep emerald flame body (team green). */
  public static final RGBWColor EMERALD = new RGBWColor(0, 60, 35);

  /** Royal blue flame body (team blue). */
  public static final RGBWColor ROYAL_BLUE = new RGBWColor(5, 20, 90);

  /** Speed of the flowing team-color patterns, in frames per second. */
  public static final double FLOW_FRAME_RATE_HZ = 12.0;

  /** Speed of the bench scan, in frames per second. */
  public static final double SCAN_FRAME_RATE_HZ = 24.0;

  // ---------- Patterns ----------

  /** Flowing emerald pattern used while disabled. */
  public final ControlRequest greenFlow = flow(EMERALD, FLOW_FRAME_RATE_HZ);

  /** Flowing royal blue pattern used while enabled. */
  public final ControlRequest blueFlow = flow(ROYAL_BLUE, FLOW_FRAME_RATE_HZ);

  /** Scrolling white scan used to verify pixel order on the bench. */
  public final ControlRequest whiteScan =
      flow(new RGBWColor(255, 255, 255), SCAN_FRAME_RATE_HZ);

  private static LEDSubsystem instance;

  /**
   * Creates the LED subsystem on the CANdle. Prefer {@link #getInstance()} so robot
   * code and commands all share the same instance.
   */
  public LEDSubsystem() {
    super(CANDLE_ID, LED_COUNT, BRIGHTNESS_SCALAR, STRIP_TYPE);
  }

  /** Returns the shared LED subsystem instance. */
  public static LEDSubsystem getInstance() {
    if (instance == null) {
      instance = new LEDSubsystem();
    }
    return instance;
  }

  /** Idle behavior while disabled: green flow. */
  public void start() {
    setBaseAnimation(greenFlow);
  }

  /** Autonomous mode: blue flow. */
  public void autoInit() {
    setBaseAnimation(blueFlow);
  }

  /** Teleoperated mode: blue flow. */
  public void teleopInit() {
    setBaseAnimation(blueFlow);
  }

  /**
   * Test mode: runs a scrolling white scan for ten seconds so the driver can verify
   * pixel order, then reverts to the base pattern.
   */
  public void testInit() {
    setAnimation(whiteScan, 10.0);
  }

  /**
   * Temporarily overrides the strip animation indefinitely, ignoring the mode base.
   *
   * @param request The animation to display.
   */
  public void setOverride(ControlRequest request) {
    setAnimation(request);
  }

  /**
   * Temporarily overrides the strip animation for a duration, after which the strip
   * returns to the mode base animation.
   *
   * @param request The animation to display.
   * @param seconds How long to display it before reverting to the base.
   */
  public void setOverride(ControlRequest request, double seconds) {
    setAnimation(request, seconds);
  }

  /** Clears any override and returns the strip to the mode base animation. */
  public void clearOverride() {
    clearAnimationOverride();
  }

  private ControlRequest flow(RGBWColor color, double frameRate) {
    return new ColorFlowAnimation(0, getLedCount() - 1)
        .withColor(color)
        .withDirection(AnimationDirectionValue.Forward)
        .withFrameRate(frameRate);
  }
}