package frc.robot.Autos;

import static frc.robot.Constants.*;

import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.auto.NamedCommands;
import com.pathplanner.lib.commands.PathPlannerAuto;
import com.pathplanner.lib.config.RobotConfig;
import com.pathplanner.lib.controllers.PPHolonomicDriveController;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.controller.ProfiledPIDController;
import edu.wpi.first.math.trajectory.TrapezoidProfile;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.WaitCommand;
import frc.robot.Drivetrain.Drivetrain;

public class AutoSelector {
    private final Drivetrain drivetrain;
    private final SendableChooser<Command> autoChooser = new SendableChooser<>();

    public AutoSelector(Drivetrain drivetrain) {
        this.drivetrain = drivetrain;

        try {
            RobotConfig config = RobotConfig.fromGUISettings();

            AutoBuilder.configure(
                drivetrain::getPose,
                drivetrain::resetPose,
                drivetrain::getCurrentChassisSpeeds,
                drivetrain::driveChassisSpeeds,
                new PPHolonomicDriveController(
                    new PIDController(5.0, 0.0, 0.0),
                    new PIDController(5.0, 0.0, 0.0),
                    new ProfiledPIDController(3.0, 0.0, 0.0,
                        new TrapezoidProfile.Constraints(
                            AUTO_MAX_ANGULAR_SPEED_RADPS, AUTO_MAX_ANGULAR_SPEED_RADPS * 2.0))
                ),
                config,
                () -> false,
                drivetrain
            );
        } catch (Exception e) {
            e.printStackTrace();
        }

        registerNamedCommands();
        buildAutoChoices();
        SmartDashboard.putData("Auto Chooser", autoChooser);
    }

    private void registerNamedCommands() {
        NamedCommands.registerCommand("resetGyro",
            new InstantCommand(drivetrain::resetGyro, drivetrain));
    }

    private void buildAutoChoices() {
        autoChooser.setDefaultOption("Do Nothing", new InstantCommand());
        autoChooser.addOption("Simple Drive Forward",
            new PathPlannerAuto("SimpleDriveForward"));
    }

    public Command getAutonomousCommand() {
        return autoChooser.getSelected();
    }
}