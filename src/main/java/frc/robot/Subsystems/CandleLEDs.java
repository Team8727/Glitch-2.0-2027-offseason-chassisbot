package frc.robot.Subsystems;

import com.ctre.phoenix6.StatusCode;
import com.ctre.phoenix6.configs.CANdleConfiguration;
import com.ctre.phoenix6.controls.SolidColor;
import com.ctre.phoenix6.hardware.CANdle;
import com.ctre.phoenix6.signals.Enable5VRailValue;
import com.ctre.phoenix6.signals.StripTypeValue;
import com.ctre.phoenix6.signals.RGBWColor;
import edu.wpi.first.wpilibj.AddressableLEDBuffer;
import edu.wpi.first.wpilibj.AddressableLEDBufferView;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.LEDPattern;
import edu.wpi.first.wpilibj.RobotController;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

import static edu.wpi.first.units.Units.Microseconds;
import static edu.wpi.first.units.Units.Seconds;

public abstract class CandleLEDs extends SubsystemBase {

  public static final double infiniteDurationSeconds = -1.0;

  public static final double ledRefreshPeriodSeconds = 0.2;

  private final CANdle candle;
  private final AddressableLEDBuffer stripBuffer;

  protected final Section strip;

  private boolean enabled = true;
  private double lastUpdateTimeMicroseconds;
  private double lastLedWriteTimeMicroseconds;
  private double deltaTimeSeconds;
  private boolean candleUnhealthyLogged = false;

  protected CandleLEDs(int candleId, int ledCount, double brightnessScalar, StripTypeValue stripType) {
    candle = new CANdle(candleId);

    CANdleConfiguration config = new CANdleConfiguration();
    config.LED.BrightnessScalar = brightnessScalar;
    config.CANdleFeatures.Enable5VRail = Enable5VRailValue.Enabled;
    config.LED.StripType = stripType;
    candle.getConfigurator().apply(config);

    stripBuffer = new AddressableLEDBuffer(ledCount);
    strip = new Section(0, ledCount - 1);
  }

  public class Section {
    private final AddressableLEDBufferView bufferView;
    private LEDPattern pattern = LEDPattern.kOff;
    private LEDPattern basePattern = LEDPattern.kOff;
    private double durationSeconds = infiniteDurationSeconds;
    private double elapsedSeconds = 0.0;

    private Section(int startIndex, int endIndex) {
      bufferView = stripBuffer.createView(startIndex, endIndex);
    }

    public void setPattern(LEDPattern pattern, double durationSeconds) {
      if (pattern == null) {
        pattern = LEDPattern.kOff;
      }
      this.pattern = pattern;
      this.durationSeconds = durationSeconds;
      this.elapsedSeconds = 0.0;
    }

    public void setPattern(LEDPattern pattern) {
      setPattern(pattern, infiniteDurationSeconds);
    }

    public void setBase(LEDPattern basePattern) {
      if (basePattern == null) {
        basePattern = LEDPattern.kOff;
      }
      this.basePattern = basePattern;
    }

    void update(double deltaTimeSeconds) {
      if (durationSeconds != infiniteDurationSeconds) {
        elapsedSeconds += deltaTimeSeconds;
        if (elapsedSeconds >= durationSeconds) {
          setPattern(basePattern);
        }
      }

      pattern.applyTo(bufferView);
    }

    public AddressableLEDBufferView getBufferView() {
      return bufferView;
    }

    public int getLength() {
      return bufferView.getLength();
    }

    public LEDPattern getCurrentPattern() {
      return pattern;
    }

    public LEDPattern getBasePattern() {
      return basePattern;
    }
  }

  public void setEnabled(boolean enabled) {
    if (!enabled && this.enabled) {
      candle.setControl(new SolidColor(0, stripBuffer.getLength() - 1).withColor(
          new RGBWColor(0, 0, 0)));
    }
    this.enabled = enabled;
  }

  public boolean isEnabled() {
    return enabled;
  }

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
      var color = new RGBWColor(
          stripBuffer.getRed(i),
          stripBuffer.getGreen(i),
          stripBuffer.getBlue(i));
      if (candle.setControl(new SolidColor(i, i).withColor(color)) != StatusCode.OK) {
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
    } else {
      candleUnhealthyLogged = false;
    }
  }
}