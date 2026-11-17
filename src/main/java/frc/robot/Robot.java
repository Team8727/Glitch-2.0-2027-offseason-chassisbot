package frc.robot;

import com.pathplanner.lib.commands.PathfindingCommand;
import dev.doglog.DogLog;
import dev.doglog.DogLogOptions;
import edu.wpi.first.wpilibj.DataLogManager;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj.RobotController;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;

/**
 * Robot lifecycle and logging.
 */
public class Robot extends TimedRobot {
    private final RobotContainer container = new RobotContainer();
    private double lastLoopStartSeconds = -1.0;
    private double loopTimeMs = 0.0;

    public Robot() {
        DataLogManager.logNetworkTables(true);
        DataLogManager.start();

        DogLog.setOptions(new DogLogOptions()
                .withCaptureConsole(true)
                .withCaptureDs(true)
                .withCaptureNt(true)
                .withLogEntryQueueCapacity(1000)
                .withLogExtras(true)
                .withNtPublish(true));
        DogLog.setEnabled(true);
        DogLog.log("Robot/SerialNumber", RobotController.getSerialNumber());

        CommandScheduler.getInstance().schedule(PathfindingCommand.warmupCommand());
    }

    @Override
    public void robotPeriodic() {
        double now = RobotController.getTime() / 1_000_000.0;
        if (lastLoopStartSeconds > 0.0) {
            loopTimeMs = (now - lastLoopStartSeconds) * 1000.0;
        }
        lastLoopStartSeconds = now;

        container.updateTelemetry(loopTimeMs);
        CommandScheduler.getInstance().run();

        DogLog.log("Robot/BatteryVoltage", RobotController.getBatteryVoltage());
        DogLog.log("Robot/VisionActive", container.getVision().isVisionActive());
        DogLog.log("Robot/VisionEnabled", container.getVision().isEnabled());
        DogLog.log("Robot/DrivetrainPoseX", container.getDrivetrain().getState().Pose.getX());
        DogLog.log("Robot/DrivetrainPoseY", container.getDrivetrain().getState().Pose.getY());
        DogLog.log(
                "Robot/DrivetrainHeadingDeg",
                container.getDrivetrain().getState().Pose.getRotation().getDegrees());
    }

    @Override
    public void disabledInit() {
        container.updateLEDs();
    }

    @Override
    public void disabledPeriodic() {
        container.trySeedPoseFromVision();
    }

    @Override
    public void autonomousInit() {
        CommandScheduler.getInstance().cancelAll();
        Command auto = container.getAutonomousCommand();
        if (auto != null) {
            CommandScheduler.getInstance().schedule(auto);
        }
    }

    @Override
    public void teleopInit() {
        CommandScheduler.getInstance().cancelAll();
    }

    @Override
    public void testInit() {
        CommandScheduler.getInstance().cancelAll();
    }

    @Override
    public void simulationPeriodic() {}

    public static boolean isRedAlliance() {
        return DriverStation.getAlliance().orElse(Alliance.Blue) == Alliance.Red;
    }
}
