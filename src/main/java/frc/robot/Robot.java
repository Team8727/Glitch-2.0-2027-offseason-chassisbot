package frc.robot;

import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import frc.robot.Subsystems.LEDSubsystem;

/** Runs only the LED subsystem currently accepted on main. */
public class Robot extends TimedRobot {
  private final LEDSubsystem leds = LEDSubsystem.getInstance();

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
    leds.autoInit();
  }

  @Override
  public void teleopInit() {
    leds.teleopInit();
  }

  @Override
  public void testInit() {
    CommandScheduler.getInstance().cancelAll();
    leds.testInit();
  }
}
