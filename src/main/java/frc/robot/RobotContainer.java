package frc.robot;

import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.auto.NamedCommands;

import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import frc.robot.subsystems.Drivetrain;

public class RobotContainer {
    private final Drivetrain drivetrain = new Drivetrain();
    private final CommandXboxController controller = new CommandXboxController(0);
    private final SendableChooser<Command> autoChooser = new SendableChooser<>();

    public RobotContainer() {
        configureNamedCommands();
        configureBindings();
        configureAutoChooser();
    }

    private void configureNamedCommands() {
        NamedCommands.registerCommand("stop", Commands.runOnce(drivetrain::stopModules, drivetrain));
    }

    private void configureBindings() {
        drivetrain.setDefaultCommand(
            Commands.run(
                () -> {
                    double forward = -controller.getLeftY();
                    double strafe = -controller.getLeftX();
                    double rotation = -controller.getRightX();

                    forward = applyDeadband(forward, 0.1);
                    strafe = applyDeadband(strafe, 0.1);
                    rotation = applyDeadband(rotation, 0.1);

                    ChassisSpeeds speeds = new ChassisSpeeds(
                        forward * Constants.Swerve.MAX_SPEED_MPS,
                        strafe * Constants.Swerve.MAX_SPEED_MPS,
                        rotation * Constants.Swerve.MAX_SPEED_MPS
                    );

                    drivetrain.drive(speeds);
                },
                drivetrain
            )
        );
    }

    private void configureAutoChooser() {
        autoChooser.setDefaultOption("Do Nothing", Commands.none());

        try {
            autoChooser.addOption("Example Auto", AutoBuilder.buildAuto("Example Auto"));
        } catch (Exception e) {
            SmartDashboard.putString("Auto Error", "Failed to load Example Auto: " + e.getMessage());
        }

        SmartDashboard.putData("Auto Mode", autoChooser);
    }

    private static double applyDeadband(double value, double deadband) {
        if (Math.abs(value) < deadband) return 0.0;
        return value;
    }

    public Command getAutonomousCommand() {
        return autoChooser.getSelected();
    }
}
