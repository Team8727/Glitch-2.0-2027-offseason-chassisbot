package frc.robot;

import com.ctre.phoenix6.swerve.SwerveModule;
import com.ctre.phoenix6.swerve.SwerveRequest;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj2.command.button.RobotModeTriggers;
import frc.robot.Autos.Autos;
import frc.robot.Commands.SteerOffsetCalibration;
import frc.robot.Drivetrain.CTRESwerveDrivetrain;
import frc.robot.Drivetrain.DrivetrainTelemetry;
import frc.robot.Drivetrain.TunerConstants;
import frc.robot.Subsystems.StatusLEDs;
import frc.robot.Vision.Vision;

/**
 * Robot-wide wiring: drivetrain, vision, LEDs, autos, and driver bindings.
 */
public class RobotContainer {
    public static final double MAX_SPEED = 0.8 * TunerConstants.kMaxLinearVelocity;
    public static final double MAX_ANGULAR_RATE = TunerConstants.kMaxAngularVelocity;

    private final CTRESwerveDrivetrain drivetrain = TunerConstants.createDrivetrain();
    private final Vision vision = new Vision();
    private final StatusLEDs leds = new StatusLEDs();
    private final Autos autos = new Autos(drivetrain);
    private final CommandXboxController controller = new CommandXboxController(0);
    private final DrivetrainTelemetry telemetry;

    private final SwerveRequest.FieldCentric fieldCentric = new SwerveRequest.FieldCentric()
            .withDeadband(MAX_SPEED * 0.1)
            .withRotationalDeadband(MAX_ANGULAR_RATE * 0.1)
            .withDriveRequestType(SwerveModule.DriveRequestType.OpenLoopVoltage);

    private final SwerveRequest.RobotCentric robotCentric = new SwerveRequest.RobotCentric()
            .withDeadband(MAX_SPEED * 0.1)
            .withRotationalDeadband(MAX_ANGULAR_RATE * 0.1)
            .withDriveRequestType(SwerveModule.DriveRequestType.OpenLoopVoltage);

    private final SwerveRequest.SwerveDriveBrake brake = new SwerveRequest.SwerveDriveBrake();
    private final SwerveRequest.Idle idle = new SwerveRequest.Idle();

    private boolean robotCentricEnabled = false;
    private boolean seededFromVision = false;

    public RobotContainer() {
        drivetrain.setVision(vision);
        telemetry = new DrivetrainTelemetry(drivetrain);

        configureBindings();
        SmartDashboard.putData(
                "Calibrate Steer Offsets", SteerOffsetCalibration.create(drivetrain));
    }

    private void configureBindings() {
        drivetrain.setDefaultCommand(drivetrain.applyRequest(() -> {
            double vx = -controller.getLeftY() * MAX_SPEED;
            double vy = -controller.getLeftX() * MAX_SPEED;
            double omega = -controller.getRightX() * MAX_ANGULAR_RATE;

            if (robotCentricEnabled) {
                return robotCentric
                        .withVelocityX(vx)
                        .withVelocityY(vy)
                        .withRotationalRate(omega);
            }
            return fieldCentric
                    .withVelocityX(vx)
                    .withVelocityY(vy)
                    .withRotationalRate(omega);
        }));

        RobotModeTriggers.disabled()
                .whileTrue(drivetrain.applyRequest(() -> idle).ignoringDisable(true));

        controller.a().whileTrue(drivetrain.applyRequest(() -> brake));
        controller.b().onTrue(Commands.runOnce(() -> {
            robotCentricEnabled = !robotCentricEnabled;
            DriverStation.reportWarning(
                    "Drive mode: " + (robotCentricEnabled ? "robot-centric" : "field-centric"),
                    false);
        }));
        controller.start().onTrue(drivetrain.runOnce(drivetrain::seedFieldCentric));
        controller.x().onTrue(drivetrain.runOnce(
                () -> vision.getLatestAcceptedPose().ifPresent(drivetrain::resetPose)));
        controller.back().onTrue(Commands.runOnce(() -> {
            vision.setEnabled(!vision.isEnabled());
            DriverStation.reportWarning(
                    "Vision fusion " + (vision.isEnabled() ? "enabled" : "disabled"), false);
        }));
    }

    public void updateLEDs() {
        if (DriverStation.isDisabled()) {
            leds.setState(StatusLEDs.State.DISABLED);
        } else if (DriverStation.isAutonomous()) {
            leds.setState(StatusLEDs.State.AUTONOMOUS);
        } else if (vision.isVisionActive()) {
            leds.setState(StatusLEDs.State.TELEOP_VISION);
        } else {
            leds.setState(StatusLEDs.State.TELEOP_ODOMETRY);
        }
    }

    public void updateTelemetry(double loopTimeMs) {
        updateLEDs();
        telemetry.publishHealth(
                edu.wpi.first.wpilibj.RobotController.getBatteryVoltage(),
                loopTimeMs,
                vision.isVisionActive(),
                vision.getLastAcceptedCount(),
                vision.isFrontLeftConnected(),
                vision.isFrontRightConnected(),
                leds.getState().name());
    }

    public void trySeedPoseFromVision() {
        if (seededFromVision) {
            return;
        }
        vision.getLatestAcceptedPose().ifPresent(pose -> {
            drivetrain.resetPose(pose);
            seededFromVision = true;
        });
    }

    public Command getAutonomousCommand() {
        return autos.getSelected();
    }

    public boolean isAutosConfigured() {
        return autos.isConfigured();
    }

    public CTRESwerveDrivetrain getDrivetrain() {
        return drivetrain;
    }

    public Vision getVision() {
        return vision;
    }
}
