package frc.robot.controller;

import com.ctre.phoenix6.swerve.SwerveModule;
import com.ctre.phoenix6.swerve.SwerveRequest;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj2.command.button.RobotModeTriggers;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.robot.Drivetrain.CTRESwerveDrivetrain;
import frc.robot.Drivetrain.Telemetry;
import frc.robot.Drivetrain.TunerConstants;

import static edu.wpi.first.units.Units.*;

/**
 * Default teleop drivetrain bindings and drive requests, mirroring the 2026
 * robot's {@code CTReSwerveControls}. Sets the drivetrain's default command
 * (field-centric Xbox drive) and binds brake/idle/reset behaviors. There is no
 * GlitchLib dependency; telemetry/URCL are the only external helpers.
 */
public class CTReSwerveControls {

  private final CTRESwerveDrivetrain drivetrain;
  private final CommandXboxController controller;

  // Max speed and angular rate for teleop control, can be tuned for better driver feel.
  public static final double MaxSpeed =
      TunerConstants.kSpeedAt12Volts.in(MetersPerSecond) * 0.8; // 80% of theoretical top speed
  public static final double MaxAngularRate =
      RotationsPerSecond.of(0.75).in(RadiansPerSecond); // 3/4 of a rotation per second max angular velocity

  // Swerve Request for normal driving; becomes the default command.
  public static final SwerveRequest.FieldCentric drive =
      new SwerveRequest.FieldCentric()
          .withDeadband(MaxSpeed * 0.1)
          .withRotationalDeadband(MaxAngularRate * 0.1) // Add a 10% deadband
          .withDriveRequestType(SwerveModule.DriveRequestType.OpenLoopVoltage); // Use open-loop control for drive motors

  /**
   * Binds the drive command and drivetrain-related behaviors to the Xbox controller.
   *
   * @param drivetrain The swerve drivetrain to control.
   * @param controller The driver's Xbox controller.
   */
  public CTReSwerveControls(CTRESwerveDrivetrain drivetrain, CommandXboxController controller) {
    this.drivetrain = drivetrain;
    this.controller = controller;

    // Note: X is forward per WPILib convention, Y is to the left.
    drivetrain.setDefaultCommand(
        drivetrain.applyRequest(
            () ->
                drive
                    .withVelocityX(controller.getLeftY() * MaxSpeed) // forward with -Y
                    .withVelocityY(controller.getLeftX() * MaxSpeed) // left with -X
                    .withRotationalRate(-controller.getRightX() * MaxAngularRate))); // CCW with -X

    final Telemetry logger = new Telemetry(MaxSpeed);

    // Idle while disabled so the configured neutral mode is applied.
    final var idle = new SwerveRequest.Idle();
    RobotModeTriggers.disabled().whileTrue(drivetrain.applyRequest(() -> idle).ignoringDisable(true));

    // Automatically brake (wheels in an X) when the sticks are all within their deadbands.
    final SwerveRequest.SwerveDriveBrake brake = new SwerveRequest.SwerveDriveBrake();
    new Trigger(
            () ->
                Math.abs(controller.getLeftY()) < 0.1
                    && Math.abs(controller.getLeftX()) < 0.1
                    && Math.abs(controller.getRightX()) < 0.1)
        .whileTrue(drivetrain.applyRequest(() -> brake));

    // Reset the field-centric heading when Start is pressed.
    controller.start().onTrue(drivetrain.runOnce(drivetrain::seedFieldCentric));

    // Point the wheels at the left-stick direction (button A + start), see 2026 repo if desired.

    drivetrain.registerTelemetry(logger::telemeterize);
  }

  /**
   * Facing-angle request helper for targeting, e.g. always facing a fixed field point.
   *
   * @param targetYawDegrees Absolute field heading in degrees to face.
   * @return A configured {@link SwerveRequest.FieldCentricFacingAngle}.
   */
  public SwerveRequest.FieldCentricFacingAngle faceAngle(double targetYawDegrees) {
    return new SwerveRequest.FieldCentricFacingAngle()
        .withDeadband(MaxSpeed * 0.1)
        .withRotationalDeadband(MaxAngularRate * 0.75)
        .withDriveRequestType(SwerveModule.DriveRequestType.OpenLoopVoltage)
        .withTargetDirection(Rotation2d.fromDegrees(targetYawDegrees))
        .withHeadingPID(8, 0, 0);
  }
}