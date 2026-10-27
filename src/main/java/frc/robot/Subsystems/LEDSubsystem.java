package frc.robot.Subsystems;

import Glitch.Lib.LEDs.GlitchLEDPatterns;
import com.ctre.phoenix6.signals.StripTypeValue;
import edu.wpi.first.wpilibj.LEDPattern;
import edu.wpi.first.wpilibj.util.Color;

import static edu.wpi.first.units.Units.Percent;
import static edu.wpi.first.units.Units.Second;

public class LEDSubsystem extends CandleLEDs {

  public static final int CANDLE_ID = 14;

  public static final int LED_COUNT = 60;

  public static final double BRIGHTNESS_SCALAR = 0.7;

  public static final StripTypeValue STRIP_TYPE = StripTypeValue.RGB;

  public static final Color EMERALD = new Color(0, 60, 35);

  public static final Color ROYAL_BLUE = new Color(5, 20, 90);

  public static final double FIRE_UPDATE_TIME_SECONDS = 0.11;

  public static final double FIRE_SPEED = 0.5;

  public final LEDPattern greenFire =
      GlitchLEDPatterns.fire(
          LEDPattern.solid(EMERALD), FIRE_UPDATE_TIME_SECONDS, Color.kGreen, FIRE_SPEED);

  public final LEDPattern blueFire =
      GlitchLEDPatterns.fire(
          LEDPattern.solid(ROYAL_BLUE), FIRE_UPDATE_TIME_SECONDS, Color.kBlue, FIRE_SPEED);

  public final LEDPattern whiteScan =
      LEDPattern.solid(Color.kWhite).scrollAtRelativeSpeed(Percent.per(Second).of(100));

  private static LEDSubsystem instance;

  public LEDSubsystem() {
    super(CANDLE_ID, LED_COUNT, BRIGHTNESS_SCALAR, STRIP_TYPE);
  }

  public static LEDSubsystem getInstance() {
    if (instance == null) {
      instance = new LEDSubsystem();
    }
    return instance;
  }

  public void start() {
    strip.setBase(greenFire);
    strip.setPattern(greenFire);
  }

  public void autoInit() {
    strip.setBase(blueFire);
    strip.setPattern(blueFire);
  }

  public void teleopInit() {
    strip.setBase(blueFire);
    strip.setPattern(blueFire);
  }

  public void testInit() {
    strip.setPattern(whiteScan, 10.0);
  }

  public void setOverride(LEDPattern pattern) {
    strip.setPattern(pattern);
  }

  public void setOverride(LEDPattern pattern, double seconds) {
    strip.setPattern(pattern, seconds);
  }

  public void clearOverride() {
    strip.setPattern(strip.getBasePattern());
  }
}