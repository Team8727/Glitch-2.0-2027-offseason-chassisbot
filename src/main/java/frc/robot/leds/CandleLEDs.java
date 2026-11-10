package frc.robot.leds;

import com.ctre.phoenix6.StatusCode;
import com.ctre.phoenix6.configs.CANdleConfiguration;
import com.ctre.phoenix6.controls.ColorFlowAnimation;
import com.ctre.phoenix6.controls.TwinkleAnimation;
import com.ctre.phoenix6.hardware.CANdle;
import com.ctre.phoenix6.signals.Enable5VRailValue;
import com.ctre.phoenix6.signals.LossOfSignalBehaviorValue;
import com.ctre.phoenix6.signals.RGBWColor;
import com.ctre.phoenix6.signals.StripTypeValue;
import frc.robot.drivetrain.TunerConstants;

/** Native CANdle animations for the 2027 SystemCore branch. */
public class CandleLEDs {
    public enum State { DISABLED, AUTONOMOUS, TELEOP_VISION, TELEOP_ODOMETRY, LOW_BATTERY }

    public static final int CANDLE_ID = 49;
    public static final int LED_COUNT = 88;
    private static final int STRIP_START = 8;
    private static final int STRIP_END = STRIP_START + LED_COUNT - 1;

    private final CANdle candle;
    private final ColorFlowAnimation base =
            new ColorFlowAnimation(STRIP_START, STRIP_END).withSlot(0).withFrameRate(35).withUpdateFreqHz(0);
    private final TwinkleAnimation sparks =
            new TwinkleAnimation(STRIP_START, STRIP_END).withSlot(1).withFrameRate(45).withUpdateFreqHz(0);

    private State applied = null;
    private boolean healthy = true;

    public CandleLEDs() {
        candle = new CANdle(CANDLE_ID, TunerConstants.kCANBus);
        CANdleConfiguration config = new CANdleConfiguration();
        config.LED.StripType = StripTypeValue.RGB;
        config.LED.BrightnessScalar = 0.6;
        config.LED.LossOfSignalBehavior = LossOfSignalBehaviorValue.KeepRunning;
        config.CANdleFeatures.Enable5VRail = Enable5VRailValue.Enabled;
        candle.getConfigurator().apply(config);
    }

    public void apply(State state) {
        if (state == applied) {
            return;
        }
        applied = state;

        RGBWColor color = switch (state) {
            case DISABLED -> new RGBWColor(0, 110, 55);
            case LOW_BATTERY -> new RGBWColor(220, 0, 0);
            default -> new RGBWColor(15, 45, 230);
        };
        boolean sparkle = state == State.AUTONOMOUS
                || state == State.TELEOP_VISION
                || state == State.TELEOP_ODOMETRY;

        base.withColor(color);
        sparks.withColor(sparkle ? new RGBWColor(150, 220, 255) : new RGBWColor())
                .withMaxLEDsOnProportion(0.28);

        healthy = candle.setControl(base) == StatusCode.OK && candle.setControl(sparks) == StatusCode.OK;
    }

    public State getApplied() {
        return applied;
    }

    public boolean isHealthy() {
        return healthy;
    }
}
