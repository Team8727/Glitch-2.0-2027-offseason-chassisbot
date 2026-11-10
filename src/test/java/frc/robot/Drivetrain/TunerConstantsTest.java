package frc.robot.Drivetrain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TunerConstantsTest {
    @Test
    void usesMk5nGearRatios() {
        assertEquals(7.03125, TunerConstants.kDriveGearRatioValue, 1e-9);
        assertEquals(287.0 / 11.0, TunerConstants.kSteerGearRatioValue, 1e-9);
    }

    @Test
    void usesExpectedFreeSpeed() {
        assertEquals(4.39, TunerConstants.kMaxLinearVelocity, 1e-9);
    }

    @Test
    void buildsFourModules() {
        assertNotNull(TunerConstants.FrontLeft);
        assertNotNull(TunerConstants.FrontRight);
        assertNotNull(TunerConstants.BackLeft);
        assertNotNull(TunerConstants.BackRight);
    }
}
