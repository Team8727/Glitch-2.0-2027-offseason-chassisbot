package frc.robot;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.robot.Autos.AutoSelector;
import frc.robot.Commands.SwerveDriveCommand;
import frc.robot.Drivetrain.Drivetrain;
import frc.robot.LEDs.LEDSubsystem;
import frc.robot.LEDs.LEDSubsystem.LEDState;
import frc.robot.Vision.VisionSubsystem;

import static frc.robot.Constants.*;

public class RobotContainer {
    private final CommandXboxController driver = new CommandXboxController(DRIVER_CONTROLLER_PORT);
    private final Drivetrain drivetrain = new Drivetrain();
    private final VisionSubsystem vision = new VisionSubsystem();
    private final LEDSubsystem leds = new LEDSubsystem();
    private final AutoSelector autoSelector;

    public RobotContainer() {
        autoSelector = new AutoSelector(drivetrain);
        configureBindings();
    }

    private void configureBindings() {
        drivetrain.setDefaultCommand(new SwerveDriveCommand(drivetrain, driver));

        // A button: reset gyro
        driver.a().onTrue(new InstantCommand(drivetrain::resetGyro, drivetrain));

        // B button: toggle field-relative
        driver.b().onTrue(new InstantCommand(drivetrain::toggleFieldRelative, drivetrain));

        // X button: toggle heading lock
        driver.x().onTrue(new InstantCommand(() -> {
            drivetrain.setFieldRelative(!drivetrain.isFieldRelative());
        }));

        // Right bumper: intake (LEDs)
        Trigger intakeTrigger = driver.rightBumper();
        intakeTrigger.onTrue(new InstantCommand(() -> leds.setState(LEDState.INTAKING)));
        intakeTrigger.onFalse(new InstantCommand(() -> leds.setState(LEDState.TELEOP)));

        // Left bumper: aiming
        Trigger aimTrigger = driver.leftBumper();
        aimTrigger.onTrue(new InstantCommand(() -> leds.setState(LEDState.AIMING)));
        aimTrigger.onFalse(new InstantCommand(() -> leds.setState(LEDState.AIMED)));
    }

    public Drivetrain getDrivetrain() {
        return drivetrain;
    }

    public VisionSubsystem getVision() {
        return vision;
    }

    public LEDSubsystem getLEDs() {
        return leds;
    }

    public Command getAutonomousCommand() {
        return autoSelector.getAutonomousCommand();
    }
}