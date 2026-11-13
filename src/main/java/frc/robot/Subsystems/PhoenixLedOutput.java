package frc.robot.Subsystems;

import com.ctre.phoenix6.configs.CANdleConfiguration;
import com.ctre.phoenix6.controls.SolidColor;
import com.ctre.phoenix6.hardware.CANdle;
import com.ctre.phoenix6.signals.Enable5VRailValue;
import com.ctre.phoenix6.signals.RGBWColor;
import com.ctre.phoenix6.signals.StripTypeValue;
import com.ctre.phoenix6.signals.VBatOutputModeValue;
import edu.wpi.first.wpilibj.DriverStation;

/** Phoenix 6 version 26.1.1 adapter. CANdle indices 0–7 are onboard; the external strip starts at 8. */
final class PhoenixLedOutput implements LedOutput {
  private final CANdle candle;

  PhoenixLedOutput(int deviceId, double brightness, StripTypeValue stripType) {
    candle = new CANdle(deviceId);
    CANdleConfiguration config = new CANdleConfiguration();
    config.LED.BrightnessScalar = brightness;
    config.LED.StripType = stripType;
    config.CANdleFeatures.Enable5VRail = Enable5VRailValue.Enabled;
    config.CANdleFeatures.VBatOutputMode = VBatOutputModeValue.On;
    var status = candle.getConfigurator().apply(config);
    if (!status.isOK()) {
      DriverStation.reportError("CANdle configuration failed: " + status, false);
    }
  }

  @Override
  public boolean setLEDs(int start, int count, int red, int green, int blue) {
    return candle.setControl(request(start, count, red, green, blue)).isOK();
  }

  /** Translate strip-relative ranges to Phoenix's inclusive device indices. */
  static SolidColor request(int start, int count, int red, int green, int blue) {
    if (start < 0 || count < 1 || start > 391 || count > 392 - start) {
      throw new IllegalArgumentException("External CANdle strip range must fit pixels 0–391");
    }
    return new SolidColor(start + 8, start + count + 7)
        .withColor(new RGBWColor(red, green, blue))
        .withUpdateFreqHz(0); // One-shot writes: the subsystem owns the refresh interval.
  }
}
