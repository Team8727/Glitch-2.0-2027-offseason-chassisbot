package frc.robot.Autos;

import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.config.PIDConstants;
import com.pathplanner.lib.config.RobotConfig;
import com.pathplanner.lib.controllers.PPHolonomicDriveController;
import com.pathplanner.lib.path.PathConstraints;
import com.pathplanner.lib.path.PathPlannerPath;
import com.ctre.phoenix6.swerve.SwerveRequest;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import frc.robot.Drivetrain.CTRESwerveDrivetrain;
import frc.robot.Drivetrain.TunerConstants;
import frc.robot.Robot;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.Set;

/**
 * PathPlanner configuration and generic offseason validation autos.
 */
public class Autos {
    private static final String DRIVE_FORWARD = "DriveForward";
    private static final String CIRCLE = "Circle";

    private final CTRESwerveDrivetrain drivetrain;
    private final SendableChooser<Command> chooser = new SendableChooser<>();
    private boolean autoBuilderConfigured = false;

    public Autos(CTRESwerveDrivetrain drivetrain) {
        this.drivetrain = drivetrain;
        configureAutoBuilder();
        buildChooser();
        SmartDashboard.putData("Auto choices", chooser);
    }

    private void configureAutoBuilder() {
        try {
            RobotConfig config = RobotConfig.fromGUISettings();
            AutoBuilder.configure(
                    () -> drivetrain.getState().Pose,
                    drivetrain::resetPose,
                    () -> drivetrain.getState().Speeds,
                    (speeds, feedforwards) -> drivetrain.setControl(
                            new SwerveRequest.ApplyRobotSpeeds().withSpeeds(speeds)),
                    new PPHolonomicDriveController(
                            new PIDConstants(5.0, 0.0, 0.0),
                            new PIDConstants(3.0, 0.0, 0.0)),
                    config,
                    Robot::isRedAlliance,
                    drivetrain);
            autoBuilderConfigured = true;
        } catch (Exception e) {
            DriverStation.reportError(
                    "Failed to configure PathPlanner. Check deploy/pathplanner/settings.json: "
                            + e.getMessage(),
                    true);
        }
    }

    private void buildChooser() {
        chooser.setDefaultOption("None", Commands.none());
        chooser.addOption("Taxi (timed)", timedTaxi());
        addPathOption("Drive Forward", DRIVE_FORWARD);
        addPathOption("Circle", CIRCLE);
        if (autoBuilderConfigured) {
            chooser.addOption(
                    "Pathfind to (3, 4, 0)",
                    AutoBuilder.pathfindToPose(
                            new Pose2d(3.0, 4.0, new Rotation2d()),
                            new PathConstraints(
                                    TunerConstants.kMaxLinearVelocity,
                                    TunerConstants.kMaxLinearAcceleration,
                                    TunerConstants.kMaxAngularVelocity,
                                    TunerConstants.kMaxAngularAcceleration)));
        }
    }

    private void addPathOption(String label, String pathName) {
        if (!autoBuilderConfigured) {
            return;
        }
        try {
            PathPlannerPath path = PathPlannerPath.fromPathFile(pathName);
            chooser.addOption(label, followPath(path));
        } catch (IOException | ParseException e) {
            DriverStation.reportError(
                    "Failed to load PathPlanner path " + pathName + ": " + e.getMessage(), false);
        }
    }

    private Command followPath(PathPlannerPath path) {
        return Commands.defer(() -> {
            PathPlannerPath selected = Robot.isRedAlliance() ? path.flipPath() : path;
            Pose2d start = selected.getStartingHolonomicPose().orElse(new Pose2d());
            return Commands.sequence(
                    Commands.runOnce(() -> drivetrain.resetPose(start)),
                    AutoBuilder.followPath(selected));
        }, Set.of(drivetrain));
    }

    private Command timedTaxi() {
        return Commands.sequence(
                        Commands.runOnce(() -> drivetrain.resetPose(
                                new Pose2d(1.5, 4.0, new Rotation2d()))),
                        Commands.run(
                                        () -> drivetrain.setControl(
                                                new SwerveRequest.ApplyRobotSpeeds()
                                                        .withSpeeds(new ChassisSpeeds(1.2, 0.0, 0.0))),
                                        drivetrain)
                                .withTimeout(1.8),
                        Commands.runOnce(() -> drivetrain.setControl(new SwerveRequest.Idle())))
                .withName("Taxi (timed)");
    }

    public String getSelectedName() {
        return getSelected().getName();
    }

    public Command getSelected() {
        Command selected = chooser.getSelected();
        return selected != null ? selected : Commands.none();
    }

    public boolean isConfigured() {
        return autoBuilderConfigured;
    }
}
