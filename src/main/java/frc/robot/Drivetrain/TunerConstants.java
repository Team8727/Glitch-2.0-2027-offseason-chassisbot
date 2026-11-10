package frc.robot.Drivetrain;

import com.ctre.phoenix6.CANBus;
import com.ctre.phoenix6.configs.*;
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
import edu.wpi.first.math.Matrix;
import edu.wpi.first.math.numbers.N1;
import edu.wpi.first.math.numbers.N3;
import edu.wpi.first.units.measure.*;

import static edu.wpi.first.units.Units.*;

/**
 * SDS MK5n swerve constants.
 *
 * <p>Drive ratio 7.03125 (7.03:1), steer ratio 287:11 (26.0909:1), 4 in wheel.
 * CAN IDs, module positions, and encoder offsets default to the 2026 robot and
 * must be verified against the 2027 chassis before driving on carpet.
 */
public class TunerConstants {
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
            .withCurrentLimits(
                    new CurrentLimitsConfigs()
                            .withStatorCurrentLimit(Amps.of(80))
                            .withSupplyCurrentLimit(Amps.of(50))
                            .withStatorCurrentLimitEnable(true)
                            .withSupplyCurrentLimitEnable(true)
            );

    private static final TalonFXConfiguration steerInitialConfigs = new TalonFXConfiguration()
            .withCurrentLimits(
                    new CurrentLimitsConfigs()
                            .withStatorCurrentLimit(Amps.of(40))
                            .withSupplyCurrentLimit(Amps.of(30))
                            .withStatorCurrentLimitEnable(true)
                            .withSupplyCurrentLimitEnable(true)
            );

    private static final CANcoderConfiguration encoderInitialConfigs = new CANcoderConfiguration();
    private static final Pigeon2Configuration pigeonConfigs = null;

    public static final CANBus kCANBus = new CANBus("Swerve Drive", "./logs/swerve.hoot");

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
            .withCANBusName(kCANBus.getName())
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
    /** TODO: calibrate with SteerOffsetCalibration. */
    public static final Angle kFrontLeftEncoderOffset = Rotations.of(0.0);
    private static final boolean kFrontLeftSteerMotorInverted = false;
    private static final boolean kFrontLeftEncoderInverted = false;

    private static final Distance kFrontLeftXPos = Inches.of(10.375);
    private static final Distance kFrontLeftYPos = Inches.of(10.375);

    // Front Right
    private static final int kFrontRightDriveMotorId = 22;
    private static final int kFrontRightSteerMotorId = 21;
    private static final int kFrontRightEncoderId = 23;
    /** TODO: calibrate with SteerOffsetCalibration. */
    public static final Angle kFrontRightEncoderOffset = Rotations.of(0.0);
    private static final boolean kFrontRightSteerMotorInverted = false;
    private static final boolean kFrontRightEncoderInverted = false;

    private static final Distance kFrontRightXPos = Inches.of(10.375);
    private static final Distance kFrontRightYPos = Inches.of(-10.375);

    // Back Left
    private static final int kBackLeftDriveMotorId = 32;
    private static final int kBackLeftSteerMotorId = 31;
    private static final int kBackLeftEncoderId = 33;
    /** TODO: calibrate with SteerOffsetCalibration. */
    public static final Angle kBackLeftEncoderOffset = Rotations.of(0.0);
    private static final boolean kBackLeftSteerMotorInverted = false;
    private static final boolean kBackLeftEncoderInverted = false;

    private static final Distance kBackLeftXPos = Inches.of(-10.375);
    private static final Distance kBackLeftYPos = Inches.of(10.375);

    // Back Right
    private static final int kBackRightDriveMotorId = 42;
    private static final int kBackRightSteerMotorId = 41;
    private static final int kBackRightEncoderId = 43;
    /** TODO: calibrate with SteerOffsetCalibration. */
    public static final Angle kBackRightEncoderOffset = Rotations.of(0.0);
    private static final boolean kBackRightSteerMotorInverted = false;
    private static final boolean kBackRightEncoderInverted = false;

    private static final Distance kBackRightXPos = Inches.of(-10.375);
    private static final Distance kBackRightYPos = Inches.of(-10.375);

    public static final double kMaxLinearVelocity = kSpeedAt12Volts.in(MetersPerSecond);
    public static final double kMaxLinearAcceleration = 2;
    public static final double kMaxAngularVelocity = RotationsPerSecond.of(0.75).in(RadiansPerSecond);
    public static final double kMaxAngularAcceleration = 0.4 * (3 * Math.PI);

    public static final double kWheelRadiusMeters = kWheelRadius.in(Meters);
    public static final double kDriveGearRatioValue = kDriveGearRatio;
    public static final double kSteerGearRatioValue = kSteerGearRatio;

    public static final SwerveModuleConstants<TalonFXConfiguration, TalonFXConfiguration, CANcoderConfiguration> FrontLeft =
            ConstantCreator.createModuleConstants(
                    kFrontLeftSteerMotorId, kFrontLeftDriveMotorId, kFrontLeftEncoderId, kFrontLeftEncoderOffset,
                    kFrontLeftXPos, kFrontLeftYPos, kInvertLeftSide, kFrontLeftSteerMotorInverted, kFrontLeftEncoderInverted
            );
    public static final SwerveModuleConstants<TalonFXConfiguration, TalonFXConfiguration, CANcoderConfiguration> FrontRight =
            ConstantCreator.createModuleConstants(
                    kFrontRightSteerMotorId, kFrontRightDriveMotorId, kFrontRightEncoderId, kFrontRightEncoderOffset,
                    kFrontRightXPos, kFrontRightYPos, kInvertRightSide, kFrontRightSteerMotorInverted, kFrontRightEncoderInverted
            );
    public static final SwerveModuleConstants<TalonFXConfiguration, TalonFXConfiguration, CANcoderConfiguration> BackLeft =
            ConstantCreator.createModuleConstants(
                    kBackLeftSteerMotorId, kBackLeftDriveMotorId, kBackLeftEncoderId, kBackLeftEncoderOffset,
                    kBackLeftXPos, kBackLeftYPos, kInvertLeftSide, kBackLeftSteerMotorInverted, kBackLeftEncoderInverted
            );
    public static final SwerveModuleConstants<TalonFXConfiguration, TalonFXConfiguration, CANcoderConfiguration> BackRight =
            ConstantCreator.createModuleConstants(
                    kBackRightSteerMotorId, kBackRightDriveMotorId, kBackRightEncoderId, kBackRightEncoderOffset,
                    kBackRightXPos, kBackRightYPos, kInvertRightSide, kBackRightSteerMotorInverted, kBackRightEncoderInverted
            );

    public static CTRESwerveDrivetrain createDrivetrain() {
        return new CTRESwerveDrivetrain(
                DrivetrainConstants, FrontLeft, FrontRight, BackLeft, BackRight);
    }

    /**
     * Swerve Drive class using Phoenix 6 with TalonFX drive/steer and CANcoder azimuth.
     */
    public static class TunerSwerveDrivetrain extends SwerveDrivetrain<TalonFX, TalonFX, CANcoder> {
        public TunerSwerveDrivetrain(
                SwerveDrivetrainConstants drivetrainConstants,
                SwerveModuleConstants<?, ?, ?>... modules
        ) {
            super(TalonFX::new, TalonFX::new, CANcoder::new, drivetrainConstants, modules);
        }

        public TunerSwerveDrivetrain(
                SwerveDrivetrainConstants drivetrainConstants,
                double odometryUpdateFrequency,
                SwerveModuleConstants<?, ?, ?>... modules
        ) {
            super(TalonFX::new, TalonFX::new, CANcoder::new, drivetrainConstants, odometryUpdateFrequency, modules);
        }

        public TunerSwerveDrivetrain(
                SwerveDrivetrainConstants drivetrainConstants,
                double odometryUpdateFrequency,
                Matrix<N3, N1> odometryStandardDeviation,
                Matrix<N3, N1> visionStandardDeviation,
                SwerveModuleConstants<?, ?, ?>... modules
        ) {
            super(TalonFX::new, TalonFX::new, CANcoder::new, drivetrainConstants, odometryUpdateFrequency,
                    odometryStandardDeviation, visionStandardDeviation, modules);
        }
    }
}
