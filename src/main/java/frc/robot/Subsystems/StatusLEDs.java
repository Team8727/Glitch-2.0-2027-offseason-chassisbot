package frc.robot.Subsystems;

import com.ctre.phoenix6.StatusCode;
import com.ctre.phoenix6.configs.CANdleConfiguration;
import com.ctre.phoenix6.controls.ColorFlowAnimation;
import com.ctre.phoenix6.controls.TwinkleAnimation;
import com.ctre.phoenix6.hardware.CANdle;
import com.ctre.phoenix6.signals.Enable5VRailValue;
import com.ctre.phoenix6.signals.LossOfSignalBehaviorValue;
import com.ctre.phoenix6.signals.RGBWColor;
import com.ctre.phoenix6.signals.StripTypeValue;
import dev.doglog.DogLog;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.RobotController;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

/**
 * CANdle status strip with a native blue-flame animation.
 *
 * <p>The CANdle renders the animation on-device, so the roboRIO only sends a
 * new control request when the state changes. This avoids the per-pixel CAN
 * traffic of the earlier software-rendered strip.
 */
public class StatusLEDs extends SubsystemBase {
    public enum State {
        DISABLED,
        AUTONOMOUS,
        TELEOP_VISION,
        TELEOP_ODOMETRY,
        LOW_BATTERY,
        FAULT
    }

    public static final int CANDLE_ID = 49;
    public static final int LED_COUNT = 88;
    public static final double BRIGHTNESS_SCALAR = 0.6;
    public static final StripTypeValue STRIP_TYPE = StripTypeValue.RGB;

    /** CANdle indices 0-7 are onboard; the external strip starts at 8. */
    private static final int STRIP_START_INDEX = 8;
    private static final int STRIP_END_INDEX = STRIP_START_INDEX + LED_COUNT - 1;
    private static final double LOW_BATTERY_VOLTS = 11.5;

    private static final RGBWColor GREEN = new RGBWColor(0, 110, 55);
    private static final RGBWColor BLUE = new RGBWColor(15, 45, 230);
    private static final RGBWColor CYAN_SPARK = new RGBWColor(150, 220, 255);
    private static final RGBWColor RED = new RGBWColor(220, 0, 0);

    private final CANdle candle;
    private final ColorFlowAnimation baseFlow =
            new ColorFlowAnimation(STRIP_START_INDEX, STRIP_END_INDEX)
                    .withSlot(0)
                    .withFrameRate(35.0)
                    .withUpdateFreqHz(0.0);
    private final TwinkleAnimation sparks =
            new TwinkleAnimation(STRIP_START_INDEX, STRIP_END_INDEX)
                    .withSlot(1)
                    .withColor(CYAN_SPARK)
                    .withMaxLEDsOnProportion(0.28)
                    .withFrameRate(45.0)
                    .withUpdateFreqHz(0.0);

    private State state = State.DISABLED;
    private State appliedState = null;
    private boolean candleFaultLogged = false;
    private boolean lastWriteHealthy = true;

    public StatusLEDs() {
        // The 2026 robot proved this CANdle is reachable on the roboRIO native CAN bus,
        // not the CANivore. Keep it native unless the strip is physically moved.
        candle = new CANdle(CANDLE_ID);

        CANdleConfiguration config = new CANdleConfiguration();
        config.LED.StripType = STRIP_TYPE;
        config.LED.BrightnessScalar = BRIGHTNESS_SCALAR;
        config.LED.LossOfSignalBehavior = LossOfSignalBehaviorValue.KeepRunning;
        config.CANdleFeatures.Enable5VRail = Enable5VRailValue.Enabled;
        candle.getConfigurator().apply(config);
    }

    public void setState(State state) {
        this.state = state;
    }

    public State getState() {
        return state;
    }

    public boolean isHealthy() {
        return lastWriteHealthy;
    }

    public static State resolveState(
            boolean disabled, boolean autonomous, boolean visionActive, double batteryVolts) {
        if (!disabled && batteryVolts < LOW_BATTERY_VOLTS) {
            return State.LOW_BATTERY;
        }
        if (disabled) {
            return State.DISABLED;
        }
        if (autonomous) {
            return State.AUTONOMOUS;
        }
        return visionActive ? State.TELEOP_VISION : State.TELEOP_ODOMETRY;
    }

    @Override
    public void periodic() {
        State effective = resolveState(
                DriverStation.isDisabled(),
                DriverStation.isAutonomous(),
                state == State.TELEOP_VISION,
                RobotController.getBatteryVoltage());

        if (effective != appliedState) {
            apply(effective);
            appliedState = effective;
        }

        if (!lastWriteHealthy && !candleFaultLogged) {
            candleFaultLogged = true;
            DriverStation.reportError(
                    "CANdle is not responding on the CANivore. Check ID " + CANDLE_ID
                            + " and the CAN bus.",
                    false);
        }

        DogLog.log("LED/State", effective.toString());
        DogLog.log("LED/Healthy", lastWriteHealthy);
    }

    private void apply(State effective) {
        RGBWColor color = switch (effective) {
            case DISABLED -> GREEN;
            case AUTONOMOUS, TELEOP_VISION, TELEOP_ODOMETRY -> BLUE;
            case LOW_BATTERY, FAULT -> RED;
        };

        baseFlow.withColor(color);
        boolean sparkle = effective == State.AUTONOMOUS
                || effective == State.TELEOP_VISION
                || effective == State.TELEOP_ODOMETRY;
        sparks.withColor(sparkle ? CYAN_SPARK : color);
        sparks.withMaxLEDsOnProportion(0.28);

        lastWriteHealthy = candle.setControl(baseFlow) == StatusCode.OK
                && candle.setControl(sparks) == StatusCode.OK;

        if (!lastWriteHealthy) {
            candleFaultLogged = false;
        }
    }
}
