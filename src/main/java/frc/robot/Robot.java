package frc.robot;

import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;

public class Robot extends TimedRobot {
    private RobotContainer robotContainer;
    private Command autoCommand;
    private boolean elasticConnected = false;

    @Override
    public void robotInit() {
        robotContainer = new RobotContainer();

        SmartDashboard.putString("drivetrain/status", "Initialized");

        // Periodic NT connection check (fixes Elastic NT issues)
        SmartDashboard.putBoolean("elastic/connected", true);
    }

    @Override
    public void robotPeriodic() {
        CommandScheduler.getInstance().run();

        // Heartbeat NT connection keepalive
        SmartDashboard.putNumber("robot/cycleTime", getPeriod());
        SmartDashboard.putBoolean("robot/connected", true);
    }

    @Override
    public void disabledInit() {
        robotContainer.getLEDs().setState(LEDs.LEDSubsystem.LEDState.DISABLED);
    }

    @Override
    public void disabledPeriodic() {}

    @Override
    public void autonomousInit() {
        robotContainer.getLEDs().setState(LEDs.LEDSubsystem.LEDState.AUTONOMOUS);
        autoCommand = robotContainer.getAutonomousCommand();
        if (autoCommand != null) {
            autoCommand.schedule();
        }
    }

    @Override
    public void autonomousPeriodic() {}

    @Override
    public void teleopInit() {
        if (autoCommand != null) {
            autoCommand.cancel();
        }
        robotContainer.getLEDs().setState(LEDs.LEDSubsystem.LEDState.TELEOP);
    }

    @Override
    public void teleopPeriodic() {
        // Feed vision measurements to pose estimator every cycle
        var frontLeft = robotContainer.getVision().getFrontLeftPose();
        var frontRight = robotContainer.getVision().getFrontRightPose();

        frontLeft.ifPresent(pose ->
            robotContainer.getDrivetrain().addVisionMeasurement(
                pose.estimatedPose.toPose2d(), pose.timestampSeconds));

        frontRight.ifPresent(pose ->
            robotContainer.getDrivetrain().addVisionMeasurement(
                pose.estimatedPose.toPose2d(), pose.timestampSeconds));
    }

    @Override
    public void testInit() {
        CommandScheduler.getInstance().cancelAll();
    }

    @Override
    public void testPeriodic() {}
}