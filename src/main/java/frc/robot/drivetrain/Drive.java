package frc.robot.drivetrain;

import com.ctre.phoenix6.swerve.SwerveDrivetrain.SwerveDriveState;
import com.ctre.phoenix6.swerve.SwerveRequest;
import org.wpilib.math.geometry.Pose2d;

/**
 * Thin drive wrapper for the 2027 SystemCore branch.
 *
 * <p>UNVERIFIED: CTRE's 26.70 alpha changed the swerve request/state types along
 * with WPILib's ChassisSpeeds -> ChassisVelocities rename. Confirm the exact
 * method names against the installed vendordep before compiling.
 */
public class Drive {
    private final SwerveDrivetrain drivetrain = TunerConstants.createDrivetrain();

    private final SwerveRequest.FieldCentric fieldCentric =
            new SwerveRequest.FieldCentric().withDeadband(0.05);
    private final SwerveRequest.RobotCentric robotCentric =
            new SwerveRequest.RobotCentric().withDeadband(0.05);
    private final SwerveRequest.Idle idle = new SwerveRequest.Idle();

    public void driveFieldRelative(double vxMetersPerSecond, double vyMetersPerSecond, double omegaRadiansPerSecond) {
        drivetrain.setControl(fieldCentric
                .withVelocityX(vxMetersPerSecond)
                .withVelocityY(vyMetersPerSecond)
                .withRotationalRate(omegaRadiansPerSecond));
    }

    public void driveRobotRelative(double vxMetersPerSecond, double vyMetersPerSecond, double omegaRadiansPerSecond) {
        drivetrain.setControl(robotCentric
                .withVelocityX(vxMetersPerSecond)
                .withVelocityY(vyMetersPerSecond)
                .withRotationalRate(omegaRadiansPerSecond));
    }

    public void stop() {
        drivetrain.setControl(idle);
    }

    public Pose2d getPose() {
        return drivetrain.getState().Pose;
    }

    public SwerveDriveState getState() {
        return drivetrain.getState();
    }

    public void addVisionMeasurement(Pose2d pose, double timestampSeconds,
            org.wpilib.math.Matrix<org.wpilib.math.numbers.N3, org.wpilib.math.numbers.N1> stdDevs) {
        drivetrain.addVisionMeasurement(pose, timestampSeconds, stdDevs);
    }

    public void resetPose(Pose2d pose) {
        drivetrain.resetPose(pose);
    }

    public void periodic() {
        drivetrain.periodic();
    }
}
