package frc.robot.LEDs;

import static frc.robot.Constants.*;

import com.ctre.phoenix.led.CANdle;
import com.ctre.phoenix.led.CANdle.LEDStripType;
import com.ctre.phoenix.led.CANdleConfiguration;
import com.ctre.phoenix.led.StrobeAnimation;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class LEDSubsystem extends SubsystemBase {
    private final CANdle candle;
    private final BlueFlameAnimation flame;
    private final Timer timer = new Timer();

    private LEDState currentState = LEDState.STARTUP;
    private double stateStartTime = 0.0;

    public enum LEDState {
        STARTUP,
        DISABLED,
        TELEOP,
        AUTONOMOUS,
        INTAKING,
        AIMING,
        AIMED,
        SHIFT_WARNING
    }

    public LEDSubsystem() {
        candle = new CANdle(CANDLE_ID, CANIVORE_BUS);
        CANdleConfiguration config = new CANdleConfiguration();
        config.stripType = LEDStripType.GRB;
        config.brightnessScalar = LED_BRIGHTNESS;
        config.vBatEnabled = false;
        config.statusLedOffWhenActive = true;
        config.disableWhenLOS = false;
        candle.configAllSettings(config);
        candle.setLEDs(0, 0, 0);

        flame = new BlueFlameAnimation(0, LED_COUNT - 1)
            .withBrightness(LED_BRIGHTNESS)
            .withSparking(FIRE_SPARKING)
            .withCooling(FIRE_COOLING)
            .withFrameRate(FIRE_FRAME_RATE);

        timer.start();
    }

    public void setState(LEDState state) {
        if (currentState != state) {
            currentState = state;
            stateStartTime = timer.get();
        }
    }

    @Override
    public void periodic() {
        if (!ENABLE_LEDS) return;

        double now = timer.get();
        boolean isRed = DriverStation.getAlliance()
            .map(a -> a == DriverStation.Alliance.Red).orElse(true);
        flame.setAlliance(isRed);

        if (DriverStation.isEStopped()) {
            candle.setLEDs(255, 0, 0);
            return;
        }

        if (DriverStation.isDisabled()) {
            // Idle flame when disabled
            int[] colors = flame.getLEDs(now);
            writeColors(colors);
            return;
        }

        if (DriverStation.isAutonomous()) {
            // Gold wave in auto
            int[] colors = flame.getLEDs(now);
            writeColors(colors);
            return;
        }

        switch (currentState) {
            case INTAKING:
                StrobeAnimation intakeAnim = new StrobeAnimation(0, 255, 0, 0, 128, LED_COUNT);
                candle.animate(intakeAnim, 0);
                break;
            case AIMING:
                StrobeAnimation aimAnim = new StrobeAnimation(
                    isRed ? 255 : 0, isRed ? 165 : 100, 0, 0, 128, LED_COUNT);
                candle.animate(aimAnim, 0);
                break;
            case AIMED:
                candle.setLEDs(0, 255, 0);
                break;
            case SHIFT_WARNING:
                StrobeAnimation shiftAnim = new StrobeAnimation(255, 255, 255, 0, 64, LED_COUNT);
                candle.animate(shiftAnim, 0);
                break;
            default:
                int[] colors = flame.getLEDs(now);
                writeColors(colors);
                break;
        }
    }

    private void writeColors(int[] colors) {
        candle.clearAnimation(0);
        for (int i = 0; i < colors.length && i < LED_COUNT; i++) {
            int color = colors[i];
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;
            candle.setLEDs(r, g, b, 0, i, 1);
        }
    }
}