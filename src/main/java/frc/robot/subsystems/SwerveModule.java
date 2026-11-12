package frc.robot.subsystems;

import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.controls.PositionVoltage;
import com.ctre.phoenix6.controls.VelocityVoltage;
import com.ctre.phoenix6.hardware.CANcoder;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.NeutralModeValue;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import frc.robot.Constants;

public class SwerveModule {
    private final TalonFX driveMotor;
    private final TalonFX steerMotor;
    private final CANcoder cancoder;

    private final VelocityVoltage driveRequest = new VelocityVoltage(0.0);
    private final PositionVoltage steerRequest = new PositionVoltage(0.0);

    private SwerveModulePosition cachedPosition = new SwerveModulePosition();

    public SwerveModule(int driveID, int steerID, int cancoderID, double encoderOffset) {
        driveMotor = new TalonFX(driveID, Constants.CAN_BUS);
        steerMotor = new TalonFX(steerID, Constants.CAN_BUS);
        cancoder = new CANcoder(cancoderID, Constants.CAN_BUS);

        configDriveMotor();
        configSteerMotor();
        syncSteerEncoder(encoderOffset);
    }

    private void configDriveMotor() {
        var cfg = new TalonFXConfiguration();

        cfg.CurrentLimits.SupplyCurrentLimit = 40;
        cfg.CurrentLimits.SupplyCurrentLimitEnable = true;

        cfg.Slot0.kP = 0.11;
        cfg.Slot0.kS = 0.25;
        cfg.Slot0.kV = 0.12;

        cfg.MotorOutput.NeutralMode = NeutralModeValue.Brake;

        driveMotor.getConfigurator().apply(cfg);
        driveMotor.setPosition(0.0);
    }

    private void configSteerMotor() {
        var cfg = new TalonFXConfiguration();

        cfg.CurrentLimits.SupplyCurrentLimit = 30;
        cfg.CurrentLimits.SupplyCurrentLimitEnable = true;

        cfg.Slot0.kP = 80.0;
        cfg.Slot0.kS = 0.25;

        cfg.MotorOutput.NeutralMode = NeutralModeValue.Brake;
        cfg.ClosedLoopGeneral.ContinuousWrap = true;

        steerMotor.getConfigurator().apply(cfg);
    }

    private void syncSteerEncoder(double offset) {
        double absPos = cancoder.getAbsolutePosition().getValueAsDouble();
        steerMotor.setPosition((absPos - offset) * Constants.Swerve.STEER_RATIO);
    }

    public void setDesiredState(SwerveModuleState state) {
        SwerveModuleState optimized = SwerveModuleState.optimize(state, getAngle());

        double motorRPS = optimized.speedMetersPerSecond / Constants.Swerve.WHEEL_CIRCUMFERENCE * Constants.Swerve.DRIVE_RATIO;
        driveMotor.setControl(driveRequest.withVelocity(motorRPS));

        double targetRotations = optimized.angle.getRotations() * Constants.Swerve.STEER_RATIO;
        steerMotor.setControl(steerRequest.withPosition(targetRotations));
    }

    public Rotation2d getAngle() {
        double motorRotations = steerMotor.getPosition().getValueAsDouble();
        return Rotation2d.fromRotations(motorRotations / Constants.Swerve.STEER_RATIO);
    }

    public SwerveModuleState getState() {
        double motorRPS = driveMotor.getVelocity().getValueAsDouble();
        double speedMPS = motorRPS / Constants.Swerve.DRIVE_RATIO * Constants.Swerve.WHEEL_CIRCUMFERENCE;
        return new SwerveModuleState(speedMPS, getAngle());
    }

    public SwerveModulePosition getPosition() {
        double motorRotations = driveMotor.getPosition().getValueAsDouble();
        cachedPosition.distanceMeters = motorRotations / Constants.Swerve.DRIVE_RATIO * Constants.Swerve.WHEEL_CIRCUMFERENCE;
        cachedPosition.angle = getAngle();
        return cachedPosition;
    }
}
