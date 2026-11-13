package frc.robot;

import edu.wpi.first.wpilibj.RobotBase;

/** WPILib entry point for the LED-only offseason baseline. */
public final class Main {
  private Main() {}

  public static void main(String... args) {
    RobotBase.startRobot(Robot::new);
  }
}
