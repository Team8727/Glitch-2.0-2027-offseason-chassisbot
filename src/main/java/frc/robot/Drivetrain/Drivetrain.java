package frc.robot.Drivetrain;

import static frc.robot.Constants.*;

import com.ctre.phoenix6.hardware.Pigeon2;

import edu.wpi.first.math.estimator.SwerveDrivePoseEstimator;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.util.sendable.SendableBuilder;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class Drivetrain extends SubsystemBase {
    private final SwerveModule[] modules;
    private final Pigeon2 pigeon;
    private final SwerveDrivePoseEstimator poseEstimator;
    private final Field2d field = new Field2d();

    private boolean isFieldRelative = true;
    private ChassisSpeeds currentSpeeds = new ChassisSpeeds();

    public Drivetrain() {
        pigeon = new Pigeon2(PIGEON_ID, CANIVORE_BUS);

        modules = new SwerveModule[] {
            new SwerveModule(FL_DRIVE_ID, FL_STEER_ID, FL_CANCODER_ID, FL_CANCODER_OFFSET,
                INVERT_LEFT_SIDE, false, false, "FL"),
            new SwerveModule(FR_DRIVE_ID, FR_STEER_ID, FR_CANCODER_ID, FR_CANCODER_OFFSET,
                INVERT_RIGHT_SIDE, false, false, "FR"),
            new SwerveModule(BL_DRIVE_ID, BL_STEER_ID, BL_CANCODER_ID, BL_CANCODER_OFFSET,
                INVERT_LEFT_SIDE, false, false, "BL"),
            new SwerveModule(BR_DRIVE_ID, BR_STEER_ID, BR_CANCODER_ID, BR_CANCODER_OFFSET,
                INVERT_RIGHT_SIDE, false, false, "BR")
        };

        for (SwerveModule m : modules) {
            m.initSteerEncoder();
        }

        poseEstimator = new SwerveDrivePoseEstimator(
            KINEMATICS, getGyroRotation(), getModulePositions(), new Pose2d());

        SmartDashboard.putData("Field", field);
    }

    public void drive(double xSpeed, double ySpeed, double rot) {
        ChassisSpeeds speeds;
        if (isFieldRelative) {
            speeds = ChassisSpeeds.fromFieldRelativeSpeeds(
                xSpeed, ySpeed, rot, getGyroRotation());
        } else {
            speeds = new ChassisSpeeds(xSpeed, ySpeed, rot);
        }

        SwerveModuleState[] states = KINEMATICS.toSwerveModuleStates(speeds);
        SwerveModuleState.desaturateWheelSpeeds(states, MAX_LINEAR_SPEED_MPS);

        for (int i = 0; i < 4; i++) {
            modules[i].setDesiredState(states[i]);
        }

        currentSpeeds = speeds;
    }

    public void driveChassisSpeeds(ChassisSpeeds speeds) {
        SwerveModuleState[] states = KINEMATICS.toSwerveModuleStates(speeds);
        SwerveModuleState.desaturateWheelSpeeds(states, MAX_LINEAR_SPEED_MPS);

        for (int i = 0; i < 4; i++) {
            modules[i].setDesiredState(states[i]);
        }

        currentSpeeds = speeds;
    }

    public ChassisSpeeds getCurrentChassisSpeeds() {
        return KINEMATICS.toChassisSpeeds(
            modules[0].getState(), modules[1].getState(),
            modules[2].getState(), modules[3].getState());
    }

    public void addVisionMeasurement(Pose2d visionPose, double timestamp) {
        poseEstimator.addVisionMeasurement(visionPose, timestamp);
    }

    public void resetGyro() {
        pigeon.setYaw(0);
    }

    public void resetPose(Pose2d pose) {
        poseEstimator.resetPosition(getGyroRotation(), getModulePositions(), pose);
        pigeon.setYaw(pose.getRotation().getDegrees());
    }

    public Pose2d getPose() {
        return poseEstimator.getEstimatedPosition();
    }

    public Rotation2d getGyroRotation() {
        return Rotation2d.fromDegrees(pigeon.getYaw().getValueAsDouble());
    }

    public double getPitch() {
        return pigeon.getPitch().getValueAsDouble();
    }

    public double getRoll() {
        return pigeon.getRoll().getValueAsDouble();
    }

    public double getGyroYaw() {
        return pigeon.getYaw().getValueAsDouble();
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

    public SwerveModule getModule(int i) {
        return modules[i];
    }

    public SwerveModule[] getModules() {
        return modules;
    }

    @Override
    public void periodic() {
        poseEstimator.update(getGyroRotation(), getModulePositions());

        // Path-based SmartDashboard keys (best practice from top teams)
        SmartDashboard.putNumber("drivetrain/pose/x", getPose().getX());
        SmartDashboard.putNumber("drivetrain/pose/y", getPose().getY());
        SmartDashboard.putNumber("drivetrain/pose/rotation", getPose().getRotation().getDegrees());
        SmartDashboard.putNumber("drivetrain/gyro/yaw", getGyroYaw());
        SmartDashboard.putNumber("drivetrain/gyro/pitch", getPitch());
        SmartDashboard.putNumber("drivetrain/gyro/roll", getRoll());

        for (int i = 0; i < 4; i++) {
            String prefix = "drivetrain/module/" + modules[i].getModuleName();
            SwerveModuleState state = modules[i].getState();
            SmartDashboard.putNumber(prefix + "/speed", state.speedMetersPerSecond);
            SmartDashboard.putNumber(prefix + "/angle", state.angle.getDegrees());
            SmartDashboard.putNumber(prefix + "/current", modules[i].getDriveCurrent());
            SmartDashboard.putNumber(prefix + "/temp", modules[i].getDriveTemperature());
        }

        field.setRobotPose(getPose());
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