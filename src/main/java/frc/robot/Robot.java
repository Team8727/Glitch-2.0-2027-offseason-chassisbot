// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import dev.doglog.DogLog;
import dev.doglog.DogLogOptions;
import edu.wpi.first.wpilibj.DataLogManager;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj.Filesystem;
import edu.wpi.first.wpilibj.RobotController;
import edu.wpi.first.net.WebServer;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import frc.robot.Drivetrain.CTRESwerveDrivetrain;
import frc.robot.Drivetrain.TunerConstants;
import frc.robot.Subsystems.LEDSubsystem;
import frc.robot.controller.CTReSwerveControls;

/**
 * The methods in this class are called automatically corresponding to each mode, as described in
 * the TimedRobot documentation. If you change the name of this class or the package after creating
 * this project, you must also update the Main.java file in the project.
 */
public class Robot extends TimedRobot {

  private Command m_autonomousCommand;

  private final CTRESwerveDrivetrain drivetrain = TunerConstants.createDrivetrain();
  private final Vision vision = new Vision();
  private final Autos autos = new Autos(drivetrain);
  private final LEDSubsystem leds = LEDSubsystem.getInstance();
  private final CTReSwerveControls controls = new CTReSwerveControls(drivetrain, new edu.wpi.first.wpilibj2.command.button.CommandXboxController(0));

  /**
   * This function is run when the robot is first started up and should be used for any
   * initialization code.
   */
  public Robot() {
    drivetrain.setVision(vision);

    // Log to a data file using WPILib's DataLogManager (baseline, always on).
    DataLogManager.start();

    // Log the NetworkTables data to the log file.
    DataLogManager.logNetworkTables(true);

    // Log DS state.
    DriverStation.startDataLog(DataLogManager.getLog());

    // DogLog for structured WPILOG + NT-mirrored logging (same as the 2026 bot).
    DogLog.setOptions(
        new DogLogOptions()
            .withCaptureDs(true)
            .withCaptureNt(true)
            .withLogExtras(true)
            .withNtPublish(true));
    DogLog.setEnabled(true);
    DogLog.log("Robot/RIO Serial Number", RobotController.getSerialNumber());

    // Log which code is actually running (git hash stamped by writeGitInfo task).
    try {
      var versionFile =
          Filesystem.getDeployDirectory().toPath().resolve("version.txt");
      DogLog.log(
          "Robot/Code Version",
          java.nio.file.Files.readString(versionFile).trim());
    } catch (Exception unused) {
      DogLog.log("Robot/Code Version", "unknown (build without writeGitInfo?)");
    }

    // Serve src/main/deploy over HTTP so Elastic can fetch the remote layout
    // (File > Load Layout From Robot / Ctrl+D). The layout must live at
    // src/main/deploy/elastic-layout.json. Fixes "Elastic can't find the layout"
    // NT connection issues: Elastic loads it straight from the robot.
    WebServer.start(5800, Filesystem.getDeployDirectory().getPath());

    // Publish loop-overrun / status info that the Elastic "Robot Status" tab shows.
    CommandScheduler.getInstance()
        .onCommandInitialize(
            c -> DogLog.log("Commands/Initialize/" + c.getName(), true));

    // Dashboard command to reset the field-centric forward heading (Auto Setup tab).
    var zeroGyro =
        drivetrain.runOnce(drivetrain::seedFieldCentric)
            .withName("Zero Gyro")
            .ignoringDisable(true);
    edu.wpi.first.wpilibj.smartdashboard.SmartDashboard.putData("Zero Gyro", zeroGyro);
  }

  /**
   * This function is called every 20 ms, no matter the mode. Use this for items like diagnostics
   * that you want ran during disabled, autonomous, teleoperated and test.
   */
  @Override
  public void robotPeriodic() {
    // Runs the Scheduler. This is responsible for polling buttons, adding newly-scheduled
    // commands, running already-scheduled commands, removing finished or interrupted commands,
    // and running subsystem periodic() methods. This must be called from the robot's periodic
    // block in order for anything in the Command-based framework to work.
    CommandScheduler.getInstance().run();

    // --- Elastic dashboard telemetry (topics consumed by elastic-layout.json) ---
    var nt = edu.wpi.first.networktables.NetworkTableInstance.getDefault();
    var table = nt.getTable("SmartDashboard");

    table.getEntry("Match Time").setDouble(DriverStation.getMatchTime());
    table.getEntry("Battery Voltage").setDouble(RobotController.getBatteryVoltage());
    table.getEntry("Alliance").setBoolean(isRedAlliance());

    var pose = drivetrain.getRobotPose();
    table.getEntry("Drive Pose X").setDouble(pose.getX());
    table.getEntry("Drive Pose Y").setDouble(pose.getY());
    table.getEntry("Drive Pose Heading").setDouble(pose.getRotation().getDegrees());
    table.getEntry("Vision Measurements").setDouble(drivetrain.getLastVisionMeasurementCount());
    table.getEntry("Vision Cameras").setDouble(2);
    // CAN bus utilization from the CANivore (0-100%).
    try {
      table.getEntry("CAN Bus Usage")
          .setDouble(TunerConstants.kCANBus.getStatus().BusUtilization * 100.0);
    } catch (Exception unused) {
      table.getEntry("CAN Bus Usage").setDouble(0.0);
    }
  }

  /** This function is called once each time the robot enters Disabled mode. */
  @Override
  public void disabledInit() {
    leds.start();
  }

  /** This function is called periodically during disabled. */
  @Override
  public void disabledPeriodic() {}

  @Override
  public void autonomousInit() {
    CommandScheduler.getInstance().cancelAll();

    // Try to seed the initial pose from visible AprilTags; fall back to the auto's own reset.
    vision.bestStartPose().ifPresent(drivetrain::resetPose);

    // Get the selected autonomous command from the Autos class.
    m_autonomousCommand = autos.getAutonomousCommand();
    if (m_autonomousCommand != null) {
      CommandScheduler.getInstance().schedule(m_autonomousCommand);
    }

    leds.autoInit();
  }

  /** This function is called periodically during autonomous. */
  @Override
  public void autonomousPeriodic() {}

  @Override
  public void teleopInit() {
    // This makes sure that autonomous stops running when teleop starts running.
    CommandScheduler.getInstance().cancelAll();

    leds.teleopInit();
  }

  /** This function is called periodically during operator control. */
  @Override
  public void teleopPeriodic() {}

  @Override
  public void testInit() {
    // Cancels all running commands at the start of test mode.
    CommandScheduler.getInstance().cancelAll();

    leds.testInit();
  }

  /** This function is called periodically during test mode. */
  @Override
  public void testPeriodic() {}

  /** This function is called once when the robot is first started up. */
  @Override
  public void simulationInit() {}

  /** This function is called periodically whilst in simulation. */
  @Override
  public void simulationPeriodic() {}

  /** Returns whether the robot is on the red alliance (defaulting to blue when uncertain). */
  public static boolean isRedAlliance() {
    return DriverStation.getAlliance().orElse(Alliance.Blue).equals(Alliance.Red);
  }
}