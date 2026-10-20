package frc.robot.Commands;

import static frc.robot.Constants.*;

import java.util.function.Supplier;

import edu.wpi.first.math.controller.ProfiledPIDController;
import edu.wpi.first.math.trajectory.TrapezoidProfile;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.Drivetrain.Drivetrain;

public class SwerveDriveCommand extends Command {
    private final Drivetrain drivetrain;
    private final Supplier<Double> xSupplier;
    private final Supplier<Double> ySupplier;
    private final Supplier<Double> rotSupplier;
    private final Supplier<Boolean> fieldRelativeSupplier;

    private final ProfiledPIDController headingController;
    private boolean headingLockActive = false;
    private double headingSetpoint = 0.0;

    private static final double HEADING_DEADBAND = 0.08;

    public SwerveDriveCommand(
            Drivetrain drivetrain,
            Supplier<Double> xSupplier,
            Supplier<Double> ySupplier,
            Supplier<Double> rotSupplier,
            Supplier<Boolean> fieldRelativeSupplier) {
        this.drivetrain = drivetrain;
        this.xSupplier = xSupplier;
        this.ySupplier = ySupplier;
        this.rotSupplier = rotSupplier;
        this.fieldRelativeSupplier = fieldRelativeSupplier;

        headingController = new ProfiledPIDController(
            0.05, 0.0, 0.0,
            new TrapezoidProfile.Constraints(
                drivetrain.getMaxAngularSpeed(), drivetrain.getMaxAngularSpeed() * 2.0));

        headingController.enableContinuousInput(-Math.PI, Math.PI);
        addRequirements(drivetrain);
    }

    @Override
    public void execute() {
        double x = applyDeadband(xSupplier.get()) * drivetrain.getMaxLinearSpeed();
        double y = applyDeadband(ySupplier.get()) * drivetrain.getMaxLinearSpeed();
        double rot = applyDeadband(rotSupplier.get()) * drivetrain.getMaxAngularSpeed();

        boolean fieldRelative = fieldRelativeSupplier.get();

        if (Math.abs(rot) < HEADING_DEADBAND * drivetrain.getMaxAngularSpeed()) {
            if (!headingLockActive) {
                headingLockActive = true;
                headingSetpoint = drivetrain.getGyroRotation().getRadians();
                headingController.reset(headingSetpoint);
            }
            rot = headingController.calculate(
                drivetrain.getGyroRotation().getRadians(), headingSetpoint);
            rot = Math.copySign(Math.min(Math.abs(rot), drivetrain.getMaxAngularSpeed()), rot);
        } else {
            headingLockActive = false;
        }

        drivetrain.drive(x, y, rot);
    }

    @Override
    public void end(boolean interrupted) {
        drivetrain.stop();
        headingLockActive = false;
    }

    private double applyDeadband(double value) {
        if (Math.abs(value) < JOYSTICK_DEADBAND) {
            return 0.0;
        }
        return value;
    }
}