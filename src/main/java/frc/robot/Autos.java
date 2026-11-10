package frc.robot;

import com.ctre.phoenix6.swerve.SwerveModule;
import com.ctre.phoenix6.swerve.SwerveRequest;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import frc.robot.Drivetrain.CTRESwerveDrivetrain;
import frc.robot.Drivetrain.TunerConstants;

import static edu.wpi.first.units.Units.RotationsPerSecond;

/**
 * Autonomous routine selection for the SystemCore (2027 alpha) build.
 *
 * <p>PathPlanner has no 2027 alpha release yet, so these are simple, robust
 * drive-only routines composed from CTRE swerve requests + timers. They cover
 * the REBUILT "leave" behavior a drive-only chassisbot needs, and are easy to
 * extend into full autos once the 2027 auto ecosystem lands.
 */
public class Autos {

  private final CTRESwerveDrivetrain drivetrain;
  private final SendableChooser<Command> autoChooser = new SendableChooser<>();

  private static final double MAX_SPEED =
      TunerConstants.kSpeedAt12Volts.in(edu.wpi.first.units.Units.MetersPerSecond);

  /**
   * Constructs an Autos instance for drive-only command autos.
   *
   * @param drivetrain The swerve drivetrain used for autonomous movement.
   */
  public Autos(CTRESwerveDrivetrain drivetrain) {
    this.drivetrain = drivetrain;
    setupAutoChooser();
    SmartDashboard.putData("Auto choices", autoChooser);
  }

  private void setupAutoChooser() {
    autoChooser.setDefaultOption("Do Nothing", Commands.none());
    autoChooser.addOption("Leave (straight)", leaveStraight(2.0));
    autoChooser.addOption("Leave (slow)", leaveStraight(3.0));
    autoChooser.addOption("Leave + Curve", leaveCurve(2.5));
  }

  /**
   * Drives straight forward at {@code seconds} at 40% speed. Field-centric, so
   * direction depends on the alliance perspective at auto start.
   */
  private Command leaveStraight(double seconds) {
    var request =
        new SwerveRequest.FieldCentric()
            .withDeadband(MAX_SPEED * 0.05)
            .withRotationalDeadband(RotationsPerSecond.of(0.1).in(edu.wpi.first.units.Units.RadiansPerSecond))
            .withDriveRequestType(SwerveModule.DriveRequestType.OpenLoopVoltage)
            .withVelocityX(MAX_SPEED * 0.4) // forward
            .withVelocityY(0)
            .withRotationalRate(0);
    return drivetrain.applyRequest(() -> request).withTimeout(seconds);
  }

  /** Drives forward while curving to the side, useful for pathing off the line. */
  private Command leaveCurve(double seconds) {
    var request =
        new SwerveRequest.FieldCentric()
            .withDeadband(MAX_SPEED * 0.05)
            .withRotationalDeadband(RotationsPerSecond.of(0.1).in(edu.wpi.first.units.Units.RadiansPerSecond))
            .withDriveRequestType(SwerveModule.DriveRequestType.OpenLoopVoltage)
            .withVelocityX(MAX_SPEED * 0.4)
            .withVelocityY(MAX_SPEED * 0.2)
            .withRotationalRate(0);
    return drivetrain.applyRequest(() -> request).withTimeout(seconds);
  }

  /** Retrieves the currently selected autonomous command from the dashboard chooser. */
  public Command getAutonomousCommand() {
    return autoChooser.getSelected();
  }
}