package frc.robot.Drivetrain;

import static frc.robot.Constants.*;

import com.ctre.phoenix6.configs.CANcoderConfiguration;
import com.ctre.phoenix6.hardware.CANcoder;
import com.ctre.phoenix6.signals.AbsoluteSensorRangeValue;
import com.ctre.phoenix6.signals.SensorDirectionValue;
import com.revrobotics.CANSparkBase;
import com.revrobotics.CANSparkBase.IdleMode;
import com.revrobotics.CANSparkLowLevel.MotorType;
import com.revrobotics.CANSparkMax;
import com.revrobotics.PeriodicFrame;
import com.revrobotics.SparkPIDController;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;

public class SwerveModule {
    private final CANSparkMax driveMotor;
    private final CANSparkMax steerMotor;
    private final CANcoder cancoder;
    private final SparkPIDController drivePID;
    private final SparkPIDController steerPID;

    private final double cancoderOffset;
    private boolean initialized = false;
    private SwerveModuleState targetState = new SwerveModuleState();

    public SwerveModule(
            int driveID, int steerID, int cancoderID,
            double cancoderOffset,
            boolean driveInverted, boolean steerInverted, boolean encoderInverted) {

        this.cancoderOffset = cancoderOffset;

        driveMotor = new CANSparkMax(driveID, MotorType.kBrushless);
        driveMotor.restoreFactoryDefaults();
        driveMotor.setInverted(driveInverted);
        driveMotor.setIdleMode(IdleMode.kBrake);
        driveMotor.setSmartCurrentLimit(60);
        driveMotor.setClosedLoopRampRate(0.2);
        driveMotor.getEncoder().setPositionConversionFactor(WHEEL_CIRCUMFERENCE / DRIVE_RATIO);
        driveMotor.getEncoder().setVelocityConversionFactor(WHEEL_CIRCUMFERENCE / DRIVE_RATIO / 60.0);
        driveMotor.setPeriodicFramePeriod(PeriodicFrame.kStatus0, 10);
        driveMotor.setPeriodicFramePeriod(PeriodicFrame.kStatus1, 20);
        driveMotor.setPeriodicFramePeriod(PeriodicFrame.kStatus2, 10);
        drivePID = driveMotor.getPIDController();
        drivePID.setP(DRIVE_P);
        drivePID.setI(DRIVE_I);
        drivePID.setD(DRIVE_D);
        drivePID.setFF(DRIVE_FF);
        drivePID.setOutputRange(-1.0, 1.0);
        driveMotor.burnFlash();

        steerMotor = new CANSparkMax(steerID, MotorType.kBrushless);
        steerMotor.restoreFactoryDefaults();
        steerMotor.setInverted(steerInverted);
        steerMotor.setIdleMode(IdleMode.kBrake);
        steerMotor.setSmartCurrentLimit(20);
        steerMotor.getEncoder().setPositionConversionFactor(1.0);
        steerMotor.getEncoder().setVelocityConversionFactor(1.0);
        steerMotor.setPeriodicFramePeriod(PeriodicFrame.kStatus0, 10);
        steerMotor.setPeriodicFramePeriod(PeriodicFrame.kStatus1, 20);
        steerMotor.setPeriodicFramePeriod(PeriodicFrame.kStatus2, 10);
        steerPID = steerMotor.getPIDController();
        steerPID.setP(STEER_P);
        steerPID.setI(STEER_I);
        steerPID.setD(STEER_D);
        steerPID.setOutputRange(-1.0, 1.0);
        steerMotor.burnFlash();

        cancoder = new CANcoder(cancoderID, CAN_BUS_NAME);
        CANcoderConfiguration config = new CANcoderConfiguration();
        config.MagnetSensor.AbsoluteSensorRange = AbsoluteSensorRangeValue.Signed_PlusMinusHalf;
        config.MagnetSensor.SensorDirection = encoderInverted
                ? SensorDirectionValue.Clockwise_Positive
                : SensorDirectionValue.CounterClockwise_Positive;
        cancoder.getConfigurator().apply(config);
    }

    public void initSteerEncoder() {
        if (initialized) return;
        initialized = true;

        double absoluteRot = cancoder.getAbsolutePosition().getValueAsDouble();
        double zeroed = absoluteRot - cancoderOffset;
        zeroed = normalizeRotations(zeroed);
        double motorRotations = zeroed * STEER_RATIO;
        steerMotor.getEncoder().setPosition(motorRotations);
    }

    public void setDesiredState(SwerveModuleState state) {
        SwerveModuleState optimized = SwerveModuleState.optimize(state, getCurrentAngle());

        double wheelRPM = optimized.speedMetersPerSecond / WHEEL_CIRCUMFERENCE * 60.0;
        double motorRPM = wheelRPM * DRIVE_RATIO;
        drivePID.setReference(motorRPM, CANSparkBase.ControlType.kVelocity);

        double targetMotorRotations = optimized.angle.getRadians() / (2.0 * Math.PI) * STEER_RATIO;
        steerPID.setReference(targetMotorRotations, CANSparkBase.ControlType.kPosition);

        targetState = optimized;
    }

    public SwerveModuleState getState() {
        double velocity = driveMotor.getEncoder().getVelocity();
        return new SwerveModuleState(velocity, getCurrentAngle());
    }

    public SwerveModulePosition getPosition() {
        double distance = driveMotor.getEncoder().getPosition();
        return new SwerveModulePosition(distance, getCurrentAngle());
    }

    public SwerveModuleState getTargetState() {
        return targetState;
    }

    public void stop() {
        driveMotor.stopMotor();
        steerMotor.stopMotor();
    }

    private Rotation2d getCurrentAngle() {
        double steerRotations = steerMotor.getEncoder().getPosition();
        double wheelRotations = steerRotations / STEER_RATIO;
        double radians = wheelRotations * 2.0 * Math.PI;
        return new Rotation2d(radians);
    }

    private static double normalizeRotations(double rot) {
        double n = rot % 1.0;
        if (n > 0.5) n -= 1.0;
        else if (n < -0.5) n += 1.0;
        return n;
    }
}