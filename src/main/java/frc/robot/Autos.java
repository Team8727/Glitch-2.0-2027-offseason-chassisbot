package frc.robot;

import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.path.PathPlannerPath;
import com.pathplanner.lib.util.FileVersionException;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.Drivetrain.CTRESwerveDrivetrain;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;

import static edu.wpi.first.wpilibj2.command.Commands.runOnce;
import static edu.wpi.first.wpilibj2.command.Commands.sequence;

/**
 * Handles the selection and execution of autonomous routines.
 *
 * <p>Loads PathPlanner paths from {@code deploy/pathplanner/paths}, publishes a
 * dashboard chooser (with mirrored variants), and resets the drivetrain pose to
 * each path's starting position before following it.
 */
public class Autos {
  private final LinkedHashMap<String, PathPlannerPath> paths = new LinkedHashMap<>();
  private final CTRESwerveDrivetrain drivetrain;
  private final SendableChooser<Command> autoChooser = new SendableChooser<>();

  /**
   * The list of autonomous path names to load and add to the auto chooser.
   * The first item in this list is set as the default option.
   */
  private static final List<String> AUTO_NAMES =
      List.of("Leave", "CenterLeave", "test", "drive test");

  /**
   * Constructs an Autos object to manage autonomous routines.
   *
   * @param drivetrain The swerve drivetrain used for autonomous movement and path following.
   */
  public Autos(CTRESwerveDrivetrain drivetrain) {
    this.drivetrain = drivetrain;

    loadPaths();
    setupAutoChooser();

    SmartDashboard.putData("Auto choices", autoChooser);
  }

  /**
   * Loads all autonomous paths from the deploy directory into the paths map.
   */
  private void loadPaths() {
    AUTO_NAMES.forEach(this::loadPath);
  }

  /**
   * Loads a specific path from the given path name.
   *
   * @param pathName The name of the path file to load.
   */
  private void loadPath(String pathName) {
    try {
      paths.put(pathName, PathPlannerPath.fromPathFile(pathName));
    } catch (IOException | ParseException e) {
      e.printStackTrace();
    } catch (FileVersionException e) {
      DriverStation.reportError(
          "Path '" + pathName + "' was saved with an incompatible PathPlanner version: " + e.getMessage(),
          e.getStackTrace());
    }
  }

  /**
   * Sets up the autonomous command chooser for the SmartDashboard.
   * This populates the chooser with each path and its mirrored version.
   */
  public void setupAutoChooser() {
    boolean first = true;
    for (var entry : paths.entrySet()) {
      PathPlannerPath path = entry.getValue();
      if (path == null) {
        continue;
      }
      Command nonMirrored = followPathFromStartPose(path, false);
      Command mirrored = followPathFromStartPose(path, true);
      if (first) {
        autoChooser.setDefaultOption(entry.getKey(), nonMirrored);
        first = false;
      } else {
        autoChooser.addOption(entry.getKey(), nonMirrored);
      }
      autoChooser.addOption(entry.getKey() + " Mirrored", mirrored);
    }
  }

  /**
   * Retrieves the currently selected autonomous command from the dashboard chooser.
   *
   * @return The selected autonomous {@link Command}.
   */
  public Command getAutonomousCommand() {
    return autoChooser.getSelected();
  }

  /**
   * Follows a PathPlanner path starting from its initial pose.
   *
   * @param path   The {@link PathPlannerPath} to follow.
   * @param mirror Whether to mirror the path based on the alliance side.
   * @return A command that resets the robot's pose to the path's start and then follows the path.
   */
  public Command followPathFromStartPose(PathPlannerPath path, boolean mirror) {
    PathPlannerPath finalPath = mirror ? path.mirrorPath() : path;
    return sequence(
        runOnce(() -> setStartPose(finalPath)),
        AutoBuilder.followPath(finalPath));
  }

  /**
   * Resets the robot's pose to the starting position of the given path,
   * accounting for alliance color when determining the initial pose.
   *
   * @param path The path to extract the starting pose from.
   */
  private void setStartPose(PathPlannerPath path) {
    Pose2d startPose =
        DriverStation.getAlliance().orElse(Alliance.Blue) == Alliance.Red
            ? path.flipPath().getStartingHolonomicPose().orElse(path.getStartingDifferentialPose())
            : path.getStartingHolonomicPose().orElse(path.getStartingDifferentialPose());

    drivetrain.resetPose(startPose);
  }
}