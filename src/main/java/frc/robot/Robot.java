package frc.robot;

import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import frc.robot.Subsystems.LEDSubsystem;

public class Robot extends TimedRobot {
  private final LEDSubsystem leds = LEDSubsystem.getInstance();

  public Robot() {
    CommandScheduler.getInstance().schedule();
  }

  @Override
  public void robotPeriodic() {
    CommandScheduler.getInstance().run();
  }

  @Override
  public void disabledInit() {
    leds.start();
  }

  @Override
  public void autonomousInit() {
    CommandScheduler.getInstance().cancelAll();
    leds.autoInit();
  }

  @Override
  public void teleopInit() {
    CommandScheduler.getInstance().cancelAll();
    leds.teleopInit();
  }

  @Override
  public void testInit() {
    CommandScheduler.getInstance().cancelAll();
    leds.testInit();
  }
}