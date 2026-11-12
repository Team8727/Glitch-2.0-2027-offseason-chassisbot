package frc.robot.Commands;

import static frc.robot.Constants.*;

import java.util.function.Supplier;

import edu.wpi.first.math.controller.ProfiledPIDController;
import edu.wpi.first.math.trajectory.TrapezoidProfile;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
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

    public SwerveDriveCommand(
            Drivetrain drivetrain,
            CommandXboxController controller) {
        this(
            drivetrain,
            () -> -controller.getLeftY(),
            () -> -controller.getLeftX(),
            () -> -controller.getRightX(),
            () -> true);
    }

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
    public void initialize() {
        headingLockActive = false;
    }

    @Override
    public void execute() {
        double rawX = applyDeadband(xSupplier.get());
        double rawY = applyDeadband(ySupplier.get());
        double rawRot = applyDeadband(rotSupplier.get());

        // Exponential joystick scaling for fine control at low speeds
        double x = Math.copySign(rawX * rawX, rawX) * drivetrain.getMaxLinearSpeed() * SPEED_MULTIPLIER;
        double y = Math.copySign(rawY * rawY, rawY) * drivetrain.getMaxLinearSpeed() * SPEED_MULTIPLIER;
        double rot = Math.copySign(rawRot * rawRot, rawRot) * drivetrain.getMaxAngularSpeed() * ROTATION_MULTIPLIER;

        boolean fieldRelative = fieldRelativeSupplier.get();

        if (ENABLE_HEADING_LOCK && Math.abs(rawRot) < HEADING_LOCK_DEADBAND) {
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