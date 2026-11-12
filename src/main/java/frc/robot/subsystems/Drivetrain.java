package frc.robot.subsystems;

import com.ctre.phoenix6.hardware.Pigeon2;
import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.config.PIDConstants;
import com.pathplanner.lib.config.RobotConfig;
import com.pathplanner.lib.controllers.PPHolonomicDriveController;
import com.pathplanner.lib.util.PathPlannerLogging;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveDriveOdometry;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;

public class Drivetrain extends SubsystemBase {
    private final Pigeon2 gyro = new Pigeon2(Constants.CAN.PIGEON_ID, Constants.CAN_BUS);

    private final SwerveModule[] modules = {
        new SwerveModule(
            Constants.CAN.FRONT_LEFT_DRIVE, Constants.CAN.FRONT_LEFT_STEER,
            Constants.CAN.FRONT_LEFT_ENCODER, Constants.Swerve.FL_OFFSET
        ),
        new SwerveModule(
            Constants.CAN.FRONT_RIGHT_DRIVE, Constants.CAN.FRONT_RIGHT_STEER,
            Constants.CAN.FRONT_RIGHT_ENCODER, Constants.Swerve.FR_OFFSET
        ),
        new SwerveModule(
            Constants.CAN.BACK_LEFT_DRIVE, Constants.CAN.BACK_LEFT_STEER,
            Constants.CAN.BACK_LEFT_ENCODER, Constants.Swerve.BL_OFFSET
        ),
        new SwerveModule(
            Constants.CAN.BACK_RIGHT_DRIVE, Constants.CAN.BACK_RIGHT_STEER,
            Constants.CAN.BACK_RIGHT_ENCODER, Constants.Swerve.BR_OFFSET
        )
    };

    private final SwerveDriveKinematics kinematics = new SwerveDriveKinematics(
        Constants.Swerve.FL_LOCATION,
        Constants.Swerve.FR_LOCATION,
        Constants.Swerve.BL_LOCATION,
        Constants.Swerve.BR_LOCATION
    );

    private final SwerveDriveOdometry odometry = new SwerveDriveOdometry(
        kinematics, getGyroYaw(), getModulePositions()
    );

    private final Field2d field2d = new Field2d();
    private RobotConfig robotConfig;

    public Drivetrain() {
        gyro.reset();

        try {
            robotConfig = RobotConfig.fromGUISettings();
        } catch (Exception e) {
            DriverStation.reportError("Failed to load PathPlanner RobotConfig", e.getStackTrace());
        }

        AutoBuilder.configure(
            this::getPose,
            this::resetOdometry,
            this::getRobotRelativeSpeeds,
            this::driveRobotRelative,
            new PPHolonomicDriveController(
                new PIDConstants(5.0, 0.0, 0.0),
                new PIDConstants(5.0, 0.0, 0.0)
            ),
            robotConfig,
            () -> {
                var alliance = DriverStation.getAlliance();
                return alliance.isPresent() && alliance.get() == Alliance.Red;
            },
            this
        );

        PathPlannerLogging.setLogActivePathCallback((poses) -> {
            field2d.getObject("path").setPoses(poses);
        });

        PathPlannerLogging.setLogTargetPoseCallback((pose) -> {
            field2d.getObject("target").setPose(pose);
        });

        SmartDashboard.putData("Field", field2d);
    }

    @Override
    public void periodic() {
        odometry.update(getGyroYaw(), getModulePositions());
        field2d.setRobotPose(getPose());
    }

    public void drive(ChassisSpeeds speeds) {
        SwerveModuleState[] states = kinematics.toSwerveModuleStates(speeds);
        SwerveDriveKinematics.desaturateWheelSpeeds(states, Constants.Swerve.MAX_SPEED_MPS);
        setModuleStates(states);
    }

    public void driveRobotRelative(ChassisSpeeds speeds) {
        drive(speeds);
    }

    public void stopModules() {
        for (var module : modules) {
            module.setDesiredState(new SwerveModuleState(0.0, module.getAngle()));
        }
    }

    public Pose2d getPose() {
        return odometry.getPoseMeters();
    }

    public void resetOdometry(Pose2d pose) {
        odometry.resetPosition(getGyroYaw(), getModulePositions(), pose);
    }

    public void zeroGyro() {
        gyro.reset();
    }

    public ChassisSpeeds getRobotRelativeSpeeds() {
        return kinematics.toChassisSpeeds(getModuleStates());
    }

    private Rotation2d getGyroYaw() {
        return Rotation2d.fromDegrees(gyro.getYaw().getValueAsDouble());
    }

    private SwerveModulePosition[] getModulePositions() {
        return new SwerveModulePosition[] {
            modules[0].getPosition(),
            modules[1].getPosition(),
            modules[2].getPosition(),
            modules[3].getPosition()
        };
    }

    private SwerveModuleState[] getModuleStates() {
        return new SwerveModuleState[] {
            modules[0].getState(),
            modules[1].getState(),
            modules[2].getState(),
            modules[3].getState()
        };
    }

    private void setModuleStates(SwerveModuleState[] states) {
        for (int i = 0; i < modules.length; i++) {
            modules[i].setDesiredState(states[i]);
        }
    }
}
