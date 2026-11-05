package frc.robot;

import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.config.PIDConstants;
import com.pathplanner.lib.config.RobotConfig;
import com.pathplanner.lib.controllers.PPHolonomicDriveController;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import frc.robot.Commands.AutonomousRoutine;
import frc.robot.Subsystems.DriveSubsystem;
import frc.robot.Subsystems.LEDSubsystem;

public class Robot extends TimedRobot {
  private final LEDSubsystem leds = LEDSubsystem.getInstance();
  private final DriveSubsystem drive = DriveSubsystem.getInstance();

  public Robot() {
    AutoBuilder.configure(
        () -> drive.getPose(),
        (pose) -> drive.resetOdometry(),
        () -> drive.getFieldSpeeds(),
        (speeds, rotation) -> {
          ChassisSpeeds fieldRelative = ChassisSpeeds.fromRobotRelativeSpeeds(speeds, rotation);
          drive.setDesiredChassisSpeeds(
              fieldRelative.vxMetersPerSecond,
              fieldRelative.vyMetersPerSecond,
              fieldRelative.omegaRadiansPerSecond);
        },
        new PPHolonomicDriveController(
            new PIDConstants(8.0, 0.0, 0.0),
            new PIDConstants(8.0, 0.0, 0.0),
            0.02),
        RobotConfig.getFalcon500(),
        () -> false,
        drive);

    CommandScheduler.getInstance().schedule();
  }

  @Override
  public void robotPeriodic() {
    CommandScheduler.getInstance().run();
  }

  @Override
  public void disabledInit() {
    drive.setAutoMode(false);
    drive.stop();
    leds.start();
  }

  @Override
  public void autonomousInit() {
    CommandScheduler.getInstance().cancelAll();
    drive.setEnabled(true);
    drive.resetOdometry();

    AutonomousRoutine autoRoutine = new AutonomousRoutine();
    autoRoutine.schedule();

    leds.autoInit();
  }

  @Override
  public void teleopInit() {
    CommandScheduler.getInstance().cancelAll();
    drive.setAutoMode(false);
    drive.stop();
    leds.teleopInit();
  }

  @Override
  public void testInit() {
    CommandScheduler.getInstance().cancelAll();
    drive.setAutoMode(false);
    drive.stop();
    leds.testInit();
  }
}