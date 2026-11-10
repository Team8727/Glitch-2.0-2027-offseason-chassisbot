package frc.robot.Subsystems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StatusLEDsTest {
    @Test
    void lowBatteryWinsOverTeleopStates() {
        assertEquals(
                StatusLEDs.State.LOW_BATTERY,
                StatusLEDs.resolveState(false, false, true, 11.0));
    }

    @Test
    void disabledWinsOverLowBattery() {
        assertEquals(
                StatusLEDs.State.DISABLED,
                StatusLEDs.resolveState(true, false, true, 10.0));
    }

    @Test
    void autoIsDistinctFromTeleop() {
        assertEquals(
                StatusLEDs.State.AUTONOMOUS,
                StatusLEDs.resolveState(false, true, false, 12.2));
    }

    @Test
    void visionStateFallsBackToOdometry() {
        assertEquals(
                StatusLEDs.State.TELEOP_VISION,
                StatusLEDs.resolveState(false, false, true, 12.2));
        assertEquals(
                StatusLEDs.State.TELEOP_ODOMETRY,
                StatusLEDs.resolveState(false, false, false, 12.2));
    }

    @Test
    void stripIsWithinCandleLimit() {
        assertTrue(StatusLEDs.LED_COUNT > 8 && StatusLEDs.LED_COUNT <= 399);
    }
}
