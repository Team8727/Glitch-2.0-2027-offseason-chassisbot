package frc.robot.Commands;

import dev.doglog.DogLog;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import frc.robot.Drivetrain.CTRESwerveDrivetrain;
import frc.robot.Vision.VisionMeasurementFilter;

/**
 * Reads each MK5n CANcoder absolute position while the wheels are mechanically
 * straight forward and logs the recommended offset values.
 *
 * <p>Procedure:
 * 1. Put the robot on blocks and point all four wheels straight forward.
 * 2. Run this command from Elastic/SmartDashboard.
 * 3. Copy SteerOffsets/FL_ROT, FR_ROT, BL_ROT, BR_ROT into TunerConstants.
 * 4. Rebuild and verify with the Drive Forward auto.
 */
public final class SteerOffsetCalibration {
    private static final String[] MODULE_NAMES = {"FL", "FR", "BL", "BR"};

    private SteerOffsetCalibration() {}

    public static Command create(CTRESwerveDrivetrain drivetrain) {
        return Commands.runOnce(() -> readAndLog(drivetrain), drivetrain)
                .withName("CalibrateSteerOffsets");
    }

    private static void readAndLog(CTRESwerveDrivetrain drivetrain) {
        for (int i = 0; i < MODULE_NAMES.length; i++) {
            double absoluteRotations = drivetrain.getModule(i)
                    .getEncoder()
                    .getAbsolutePosition()
                    .getValueAsDouble();
            double recommendedOffset = VisionMeasurementFilter.normalizeOffsetRotations(
                    absoluteRotations);

            String name = MODULE_NAMES[i];
            SmartDashboard.putNumber("SteerOffsets/" + name + "_ABS_ROT", absoluteRotations);
            SmartDashboard.putNumber("SteerOffsets/" + name + "_ROT", recommendedOffset);
            SmartDashboard.putNumber("SteerOffsets/" + name + "_ROT_ALT", -recommendedOffset);

            DogLog.log("SteerOffsets/" + name + "_ABS_ROT", absoluteRotations);
            DogLog.log("SteerOffsets/" + name + "_ROT", recommendedOffset);
            DogLog.log("SteerOffsets/" + name + "_ROT_ALT", -recommendedOffset);
        }
        DogLog.log("SteerOffsets/CalibrationRan", true);
    }
}
