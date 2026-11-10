package frc.robot;

import frc.robot.autos.AutoChooser;
import frc.robot.drivetrain.Drive;
import frc.robot.leds.CandleLEDs;
import frc.robot.telemetry.Telemetry;
import org.wpilib.framework.RobotController;
import org.wpilib.framework.TimedRobot;
import org.wpilib.driverstation.Gamepad;

/**
 * Beako-Syscore robot. Drive-only REBUILT chassis on WPILib 2027 / SystemCore.
 *
 * <p>UNVERIFIED: written against the 2027 alpha docs before a SystemCore and the
 * 2027 toolchain were available. Compile only after installing WPILib 2027
 * alpha 8 and the matching vendor libraries.
 */
public class Robot extends TimedRobot {
    private final Drive drive = new Drive();
    private final Telemetry telemetry = new Telemetry();
    private final CandleLEDs leds = new CandleLEDs();
    private final Gamepad driver = new Gamepad(0);
    private AutoChooser autos;

    private double lastLoopStart = -1.0;
    private double loopMs = 0.0;

    private static final double MAX_SPEED = 4.39;
    private static final double MAX_OMEGA = 0.75 * 2.0 * Math.PI;
    private static final double SLOW_SPEED = 0.35;

    public Robot() {
        autos = new AutoChooser(drive);
    }

    @Override
    public void robotPeriodic() {
        double now = RobotController.getTime() / 1_000_000_000.0;
        if (lastLoopStart > 0.0) {
            loopMs = (now - lastLoopStart) * 1000.0;
        }
        lastLoopStart = now;

        if (isDisabled()) {
            leds.apply(CandleLEDs.State.DISABLED);
        } else if (isAutonomous()) {
            leds.apply(CandleLEDs.State.AUTONOMOUS);
        } else {
            leds.apply(CandleLEDs.State.TELEOP_ODOMETRY);
        }

        telemetry.publishDrive(drive.getPose());
        telemetry.publishHealth(
                RobotController.getBatteryVoltage(),
                loopMs,
                false,
                0,
                String.valueOf(leds.getApplied()));
    }

    @Override
    public void autonomousInit() {
        autos.select();
        autos.start();
    }

    @Override
    public void autonomousPeriodic() {
        autos.run();
    }

    @Override
    public void autonomousExit() {
        autos.end();
    }

    @Override
    public void teleopInit() {
        drive.stop();
    }

    @Override
    public void teleopPeriodic() {
        double scale = driver.getRightBumperButton() ? SLOW_SPEED : 1.0;
        double vx = -driver.getLeftY() * MAX_SPEED * scale;
        double vy = -driver.getLeftX() * MAX_SPEED * scale;
        double omega = -driver.getRightX() * MAX_OMEGA * scale;

        if (driver.getBButton()) {
            drive.driveRobotRelative(vx, vy, omega);
        } else {
            drive.driveFieldRelative(vx, vy, omega);
        }
    }

    @Override
    public void disabledInit() {
        drive.stop();
    }

    @Override
    public void simulationPeriodic() {
        drive.periodic();
    }
}
