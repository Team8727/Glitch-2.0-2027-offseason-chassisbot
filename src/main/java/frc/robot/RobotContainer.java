package frc.robot;

import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.button.JoystickButton;
import frc.robot.Commands.SwerveDriveCommand;
import frc.robot.Drivetrain.Drivetrain;

public class RobotContainer {
    private final XboxController driver = new XboxController(0);
    private final Drivetrain drivetrain = new Drivetrain();

    public RobotContainer() {
        configureBindings();
    }

    private void configureBindings() {
        drivetrain.setDefaultCommand(new SwerveDriveCommand(
            drivetrain,
            () -> -driver.getLeftY(),
            () -> -driver.getLeftX(),
            () -> -driver.getRightX(),
            () -> true
        ));

        new JoystickButton(driver, XboxController.Button.kA.value)
            .onTrue(new InstantCommand(drivetrain::resetGyro, drivetrain));
    }

    public Command getAutonomousCommand() {
        return new InstantCommand();
    }
}