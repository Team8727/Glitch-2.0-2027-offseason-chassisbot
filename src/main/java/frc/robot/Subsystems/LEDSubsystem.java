// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.Subsystems;

import Glitch.Lib.LEDs.GlitchLEDPatterns;
import com.ctre.phoenix6.configs.LedStripType;
import edu.wpi.first.wpilibj.LEDPattern;
import edu.wpi.first.wpilibj.util.Color;

import static edu.wpi.first.units.Units.Percent;
import static edu.wpi.first.units.Units.Second;

/**
 * Team LED subsystem for the offseason chassisbot.
 *
 * <p>Drives the CTRE CANdle with green/blue fire patterns built on top of
 * GlitchLib's {@code GlitchLEDPatterns.fire(...)} overlay. The strip is a single
 * continuous section of {@value #LED_COUNT} LEDs, so all of the tunable values below
 * live in one block for easy on-bench adjustment.
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
  public static final LedStripType STRIP_TYPE = LedStripType.kRGB;

  /** Deep emerald flame body (team green). */
  public static final Color EMERALD = new Color(0, 60, 35);

  /** Royal blue flame body (team blue). */
  public static final Color ROYAL_BLUE = new Color(5, 20, 90);

  /** Seconds between fire overlay updates. */
  public static final double FIRE_UPDATE_TIME_SECONDS = 0.11;

  /** Fire wave speed; 1.0 is normal, lower is slower. */
  public static final double FIRE_SPEED = 0.5;

  // ---------- Patterns ----------

  /** Fire overlay over an emerald body, tinted green toward the flame tips. */
  public final LEDPattern greenFire =
      GlitchLEDPatterns.fire(
          LEDPattern.solid(EMERALD), FIRE_UPDATE_TIME_SECONDS, Color.kGreen, FIRE_SPEED);

  /** Fire overlay over a royal blue body, tinted blue toward the flame tips. */
  public final LEDPattern blueFire =
      GlitchLEDPatterns.fire(
          LEDPattern.solid(ROYAL_BLUE), FIRE_UPDATE_TIME_SECONDS, Color.kBlue, FIRE_SPEED);

  /** Scrolling white scan used to verify pixel order on the bench. */
  public final LEDPattern whiteScan =
      LEDPattern.solid(Color.kWhite).scrollAtRelativeSpeed(Percent.per(Second).of(100));

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

  /** Idle behavior while disabled: green fire. */
  public void start() {
    strip.setBase(greenFire);
    strip.setPattern(greenFire);
  }

  /** Autonomous mode: blue fire. */
  public void autoInit() {
    strip.setBase(blueFire);
    strip.setPattern(blueFire);
  }

  /** Teleoperated mode: blue fire. */
  public void teleopInit() {
    strip.setBase(blueFire);
    strip.setPattern(blueFire);
  }

  /**
   * Test mode: runs a scrolling white scan for ten seconds so the driver can verify
   * pixel order, then reverts to the base pattern.
   */
  public void testInit() {
    strip.setPattern(whiteScan, 10.0);
  }

  /**
   * Temporarily overrides the strip pattern indefinitely, ignoring the mode base.
   *
   * @param pattern The pattern to display.
   */
  public void setOverride(LEDPattern pattern) {
    strip.setPattern(pattern);
  }

  /**
   * Temporarily overrides the strip pattern for a duration, after which the strip
   * returns to the mode base pattern.
   *
   * @param pattern The pattern to display.
   * @param seconds How long to display it before reverting to the base.
   */
  public void setOverride(LEDPattern pattern, double seconds) {
    strip.setPattern(pattern, seconds);
  }

  /** Clears any override and returns the strip to the mode base pattern. */
  public void clearOverride() {
    strip.setPattern(strip.getBasePattern());
  }
}