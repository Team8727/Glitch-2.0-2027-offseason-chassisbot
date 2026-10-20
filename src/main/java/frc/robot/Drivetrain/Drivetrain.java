package frc.robot.Drivetrain;

import static frc.robot.Constants.*;

import com.ctre.phoenix6.hardware.Pigeon2;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveOdometry;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class Drivetrain extends SubsystemBase {
    private final SwerveModule[] modules;
    private final Pigeon2 pigeon;
    private final SwerveDriveOdometry odometry;
    private final Field2d field = new Field2d();

    private boolean isFieldRelative = true;

    public Drivetrain() {
        pigeon = new Pigeon2(PIGEON_ID, CAN_BUS_NAME);

        modules = new SwerveModule[] {
            new SwerveModule(
                FL_DRIVE_ID, FL_STEER_ID, FL_CANCODER_ID, FL_CANCODER_OFFSET,
                INVERT_LEFT_SIDE, false, false),
            new SwerveModule(
                FR_DRIVE_ID, FR_STEER_ID, FR_CANCODER_ID, FR_CANCODER_OFFSET,
                INVERT_RIGHT_SIDE, false, false),
            new SwerveModule(
                BL_DRIVE_ID, BL_STEER_ID, BL_CANCODER_ID, BL_CANCODER_OFFSET,
                INVERT_LEFT_SIDE, false, false),
            new SwerveModule(
                BR_DRIVE_ID, BR_STEER_ID, BR_CANCODER_ID, BR_CANCODER_OFFSET,
                INVERT_RIGHT_SIDE, false, false)
        };

        for (SwerveModule m : modules) {
            m.initSteerEncoder();
        }

        odometry = new SwerveDriveOdometry(
            KINEMATICS, getGyroRotation(), getModulePositions(), new Pose2d());
    }

    public void drive(double xSpeed, double ySpeed, double rot) {
        ChassisSpeeds speeds;
        if (isFieldRelative) {
            speeds = ChassisSpeeds.fromFieldRelativeSpeeds(
                xSpeed, -ySpeed, rot, getGyroRotation());
        } else {
            speeds = new ChassisSpeeds(xSpeed, -ySpeed, rot);
        }

        SwerveModuleState[] states = KINEMATICS.toSwerveModuleStates(speeds);
        SwerveModuleState.desaturateWheelSpeeds(states, MAX_LINEAR_SPEED_MPS);

        for (int i = 0; i < 4; i++) {
            modules[i].setDesiredState(states[i]);
        }
    }

    public void resetGyro() {
        pigeon.setYaw(0);
    }

    public void resetOdometry(Pose2d pose) {
        odometry.resetPosition(getGyroRotation(), getModulePositions(), pose);
        pigeon.setYaw(pose.getRotation().getDegrees());
    }

    public Pose2d getPose() {
        return odometry.getPoseMeters();
    }

    public Rotation2d getGyroRotation() {
        return Rotation2d.fromDegrees(pigeon.getYaw().getValueAsDouble());
    }

    public double getMaxAngularSpeed() {
        return MAX_ANGULAR_SPEED_RADPS;
    }

    public double getMaxLinearSpeed() {
        return MAX_LINEAR_SPEED_MPS;
    }

    public void toggleFieldRelative() {
        isFieldRelative = !isFieldRelative;
    }

    public void setFieldRelative(boolean fieldRelative) {
        isFieldRelative = fieldRelative;
    }

    public boolean isFieldRelative() {
        return isFieldRelative;
    }

    @Override
    public void periodic() {
        odometry.update(getGyroRotation(), getModulePositions());

        SmartDashboard.putNumber("Gyro Yaw", getGyroRotation().getDegrees());
        SmartDashboard.putNumber("Pose X", getPose().getX());
        SmartDashboard.putNumber("Pose Y", getPose().getY());
        SmartDashboard.putNumber("Pose Heading", getPose().getRotation().getDegrees());

        for (int i = 0; i < 4; i++) {
            SwerveModuleState state = modules[i].getState();
            SmartDashboard.putNumber("Module " + i + " Speed", state.speedMetersPerSecond);
            SmartDashboard.putNumber("Module " + i + " Angle", state.angle.getDegrees());
        }

        field.setRobotPose(getPose());
        SmartDashboard.putData("Field", field);
    }

    public void stop() {
        for (SwerveModule m : modules) {
            m.stop();
        }
    }

    private SwerveModulePosition[] getModulePositions() {
        SwerveModulePosition[] positions = new SwerveModulePosition[4];
        for (int i = 0; i < 4; i++) {
            positions[i] = modules[i].getPosition();
        }
        return positions;
    }
}