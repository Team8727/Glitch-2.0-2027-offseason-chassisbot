package frc.robot.drivetrain;

import static org.wpilib.units.Units.*;

import com.ctre.phoenix6.CANBus;
import com.ctre.phoenix6.configs.CANcoderConfiguration;
import com.ctre.phoenix6.configs.CurrentLimitsConfigs;
import com.ctre.phoenix6.configs.MotorOutputConfigs;
import com.ctre.phoenix6.configs.Pigeon2Configuration;
import com.ctre.phoenix6.configs.Slot0Configs;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.hardware.CANcoder;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.StaticFeedforwardSignValue;
import com.ctre.phoenix6.swerve.SwerveDrivetrain;
import com.ctre.phoenix6.swerve.SwerveDrivetrainConstants;
import com.ctre.phoenix6.swerve.SwerveModuleConstants;
import com.ctre.phoenix6.swerve.SwerveModuleConstants.ClosedLoopOutputType;
import com.ctre.phoenix6.swerve.SwerveModuleConstants.DriveMotorArrangement;
import com.ctre.phoenix6.swerve.SwerveModuleConstants.SteerFeedbackType;
import com.ctre.phoenix6.swerve.SwerveModuleConstants.SteerMotorArrangement;
import com.ctre.phoenix6.swerve.SwerveModuleConstantsFactory;
import org.wpilib.math.Matrix;
import org.wpilib.math.numbers.N1;
import org.wpilib.math.numbers.N3;
import org.wpilib.units.measure.Angle;
import org.wpilib.units.measure.Current;
import org.wpilib.units.measure.Distance;
import org.wpilib.units.measure.LinearVelocity;
import org.wpilib.units.measure.MomentOfInertia;
import org.wpilib.units.measure.Voltage;

/**
 * 2027 SystemCore swerve constants.
 *
 * <p>Mechanical values are the 2027 chassis values. CAN IDs and encoder offsets
 * still come from the 2026 robot and must be verified before driving.
 *
 * <p>STILL UNVERIFIED: the CTRE 26.70 alpha renamed the swerve network field to
 * {@code Network} and changed CAN bus construction to {@link CANBus}. Confirm
 * against the vendordep you install.
 */
public final class TunerConstants {
    private TunerConstants() {}

    private static final Slot0Configs steerGains = new Slot0Configs()
            .withKP(100).withKI(0).withKD(0.5)
            .withKS(0.1).withKV(2.49).withKA(0)
            .withStaticFeedforwardSign(StaticFeedforwardSignValue.UseClosedLoopSign);

    private static final Slot0Configs driveGains = new Slot0Configs()
            .withKP(0.1).withKI(0).withKD(0)
            .withKS(0).withKV(0.124);

    private static final ClosedLoopOutputType kSteerClosedLoopOutput = ClosedLoopOutputType.Voltage;
    private static final ClosedLoopOutputType kDriveClosedLoopOutput = ClosedLoopOutputType.Voltage;
    private static final DriveMotorArrangement kDriveMotorType = DriveMotorArrangement.TalonFX_Integrated;
    private static final SteerMotorArrangement kSteerMotorType = SteerMotorArrangement.TalonFX_Integrated;
    private static final SteerFeedbackType kSteerFeedbackType = SteerFeedbackType.FusedCANcoder;

    private static final Current kSlipCurrent = Amps.of(80);

    private static final TalonFXConfiguration driveInitialConfigs = new TalonFXConfiguration()
            .withCurrentLimits(new CurrentLimitsConfigs()
                    .withStatorCurrentLimit(Amps.of(80))
                    .withSupplyCurrentLimit(Amps.of(50))
                    .withStatorCurrentLimitEnable(true)
                    .withSupplyCurrentLimitEnable(true));

    private static final TalonFXConfiguration steerInitialConfigs = new TalonFXConfiguration()
            .withCurrentLimits(new CurrentLimitsConfigs()
                    .withStatorCurrentLimit(Amps.of(40))
                    .withSupplyCurrentLimit(Amps.of(30))
                    .withStatorCurrentLimitEnable(true)
                    .withSupplyCurrentLimitEnable(true))
            .withMotorOutput(new MotorOutputConfigs());

    private static final CANcoderConfiguration encoderInitialConfigs = new CANcoderConfiguration();
    private static final Pigeon2Configuration pigeonConfigs = null;

    /**
     * SystemCore default bus is {@code can_s1}. If the CANivore is kept, construct
     * it with the CANivore's name instead and verify the bus in Tuner X.
     */
    public static final CANBus kCANBus = new CANBus();

    public static final LinearVelocity kSpeedAt12Volts = MetersPerSecond.of(4.39);

    private static final double kCoupleRatio = 4.5;
    private static final double kDriveGearRatio = 7.03125;
    private static final double kSteerGearRatio = 287.0 / 11.0;
    private static final Distance kWheelRadius = Inches.of(2);

    private static final boolean kInvertLeftSide = false;
    private static final boolean kInvertRightSide = true;

    private static final int kPigeonId = 2;

    private static final MomentOfInertia kSteerInertia = KilogramSquareMeters.of(0.01);
    private static final MomentOfInertia kDriveInertia = KilogramSquareMeters.of(0.01);
    private static final Voltage kSteerFrictionVoltage = Volts.of(0.2);
    private static final Voltage kDriveFrictionVoltage = Volts.of(0.2);

    public static final SwerveDrivetrainConstants DrivetrainConstants = new SwerveDrivetrainConstants()
            .withNetwork(kCANBus)
            .withPigeon2Id(kPigeonId)
            .withPigeon2Configs(pigeonConfigs);

    private static final SwerveModuleConstantsFactory<TalonFXConfiguration, TalonFXConfiguration, CANcoderConfiguration> ConstantCreator =
            new SwerveModuleConstantsFactory<TalonFXConfiguration, TalonFXConfiguration, CANcoderConfiguration>()
                    .withDriveMotorGearRatio(kDriveGearRatio)
                    .withSteerMotorGearRatio(kSteerGearRatio)
                    .withCouplingGearRatio(kCoupleRatio)
                    .withWheelRadius(kWheelRadius)
                    .withSteerMotorGains(steerGains)
                    .withDriveMotorGains(driveGains)
                    .withSteerMotorClosedLoopOutput(kSteerClosedLoopOutput)
                    .withDriveMotorClosedLoopOutput(kDriveClosedLoopOutput)
                    .withSlipCurrent(kSlipCurrent)
                    .withSpeedAt12Volts(kSpeedAt12Volts)
                    .withDriveMotorType(kDriveMotorType)
                    .withSteerMotorType(kSteerMotorType)
                    .withFeedbackSource(kSteerFeedbackType)
                    .withDriveMotorInitialConfigs(driveInitialConfigs)
                    .withSteerMotorInitialConfigs(steerInitialConfigs)
                    .withEncoderInitialConfigs(encoderInitialConfigs)
                    .withSteerInertia(kSteerInertia)
                    .withDriveInertia(kDriveInertia)
                    .withSteerFrictionVoltage(kSteerFrictionVoltage)
                    .withDriveFrictionVoltage(kDriveFrictionVoltage);

    // Front Left
    private static final int kFrontLeftDriveMotorId = 12;
    private static final int kFrontLeftSteerMotorId = 11;
    private static final int kFrontLeftEncoderId = 13;
    public static final Angle kFrontLeftEncoderOffset = Rotations.of(0.0);
    private static final Distance kFrontLeftXPos = Inches.of(10.375);
    private static final Distance kFrontLeftYPos = Inches.of(10.375);

    // Front Right
    private static final int kFrontRightDriveMotorId = 22;
    private static final int kFrontRightSteerMotorId = 21;
    private static final int kFrontRightEncoderId = 23;
    public static final Angle kFrontRightEncoderOffset = Rotations.of(0.0);
    private static final Distance kFrontRightXPos = Inches.of(10.375);
    private static final Distance kFrontRightYPos = Inches.of(-10.375);

    // Back Left
    private static final int kBackLeftDriveMotorId = 32;
    private static final int kBackLeftSteerMotorId = 31;
    private static final int kBackLeftEncoderId = 33;
    public static final Angle kBackLeftEncoderOffset = Rotations.of(0.0);
    private static final Distance kBackLeftXPos = Inches.of(-10.375);
    private static final Distance kBackLeftYPos = Inches.of(10.375);

    // Back Right
    private static final int kBackRightDriveMotorId = 42;
    private static final int kBackRightSteerMotorId = 41;
    private static final int kBackRightEncoderId = 43;
    public static final Angle kBackRightEncoderOffset = Rotations.of(0.0);
    private static final Distance kBackRightXPos = Inches.of(-10.375);
    private static final Distance kBackRightYPos = Inches.of(-10.375);

    public static final double kMaxLinearVelocity = kSpeedAt12Volts.in(MetersPerSecond);
    public static final double kMaxLinearAcceleration = 2.0;
    public static final double kMaxAngularVelocity = RotationsPerSecond.of(0.75).in(RadiansPerSecond);
    public static final double kMaxAngularAcceleration = 0.4 * (3 * Math.PI);

    public static final SwerveModuleConstants<TalonFXConfiguration, TalonFXConfiguration, CANcoderConfiguration> FrontLeft =
            ConstantCreator.createModuleConstants(
                    kFrontLeftSteerMotorId, kFrontLeftDriveMotorId, kFrontLeftEncoderId,
                    kFrontLeftEncoderOffset, kFrontLeftXPos, kFrontLeftYPos,
                    kInvertLeftSide, false, false);
    public static final SwerveModuleConstants<TalonFXConfiguration, TalonFXConfiguration, CANcoderConfiguration> FrontRight =
            ConstantCreator.createModuleConstants(
                    kFrontRightSteerMotorId, kFrontRightDriveMotorId, kFrontRightEncoderId,
                    kFrontRightEncoderOffset, kFrontRightXPos, kFrontRightYPos,
                    kInvertRightSide, false, false);
    public static final SwerveModuleConstants<TalonFXConfiguration, TalonFXConfiguration, CANcoderConfiguration> BackLeft =
            ConstantCreator.createModuleConstants(
                    kBackLeftSteerMotorId, kBackLeftDriveMotorId, kBackLeftEncoderId,
                    kBackLeftEncoderOffset, kBackLeftXPos, kBackLeftYPos,
                    kInvertLeftSide, false, false);
    public static final SwerveModuleConstants<TalonFXConfiguration, TalonFXConfiguration, CANcoderConfiguration> BackRight =
            ConstantCreator.createModuleConstants(
                    kBackRightSteerMotorId, kBackRightDriveMotorId, kBackRightEncoderId,
                    kBackRightEncoderOffset, kBackRightXPos, kBackRightYPos,
                    kInvertRightSide, false, false);

    public static SwerveDrivetrain<TalonFX, TalonFX, CANcoder> createDrivetrain() {
        return new SwerveDrivetrain<>(
                TalonFX::new, TalonFX::new, CANcoder::new,
                DrivetrainConstants, FrontLeft, FrontRight, BackLeft, BackRight);
    }
}
