package frc.robot.Subsystems;

import com.ctre.phoenix6.configs.TalonSRXConfiguration;
import com.ctre.phoenix6.hardware.CANcoder;
import com.ctre.phoenix6.hardware.TalonSRX;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.geometry.Translation2d;
import static edu.wpi.first.units.Units.MetersPerSecond;
import static edu.wpi.first.units.Units.Rotations;
import static edu.wpi.first.units.Units.RotationsPerSecond;

public class SwerveModule {
  private static final double STEER_P = 8.0;
  private static final double STEER_I = 0.0;
  private static final double STEER_D = 0.05;
  private static final double DRIVE_P = 2.0;
  private static final double DRIVE_I = 0.0;
  private static final double DRIVE_D = 0.0;
  private static final double WHEEL_DIAMETER_METERS = 0.1016;

  private static final double WHEEL_CIRCUMFERENCE_METERS = WHEEL_DIAMETER_METERS * Math.PI;

  private final TalonSRX driveMotor;
  private final TalonSRX steerMotor;
  private final CANcoder encoder;
  private final PIDController steerPID;
  private final PIDController drivePID;
  private final String label;

  private double targetSpeedMps;
  private double targetAngleRad;
  private boolean enabled;

  public SwerveModule(int driveCanId, int steerCanId, int encoderCanId, String label) {
    this.label = label;
    driveMotor = new TalonSRX(driveCanId);
    steerMotor = new TalonSRX(steerCanId);
    encoder = new CANcoder(encoderCanId);

    steerPID = new PIDController(STEER_P, STEER_I, STEER_D);
    drivePID = new PIDController(DRIVE_P, DRIVE_I, DRIVE_D);

    TalonSRXConfiguration driveConfig = new TalonSRXConfiguration();
    driveConfig.forwardSoftLimitThreshold = 5.0;
    driveConfig.reverseSoftLimitThreshold = -5.0;
    driveConfig.forwardSoftLimitEnable = true;
    driveConfig.reverseSoftLimitEnable = true;
    driveMotor.getConfigurator().apply(driveConfig);

    TalonSRXConfiguration steerConfig = new TalonSRXConfiguration();
    steerConfig.forwardSoftLimitThreshold = 5.0;
    steerConfig.reverseSoftLimitThreshold = -5.0;
    steerConfig.forwardSoftLimitEnable = true;
    steerConfig.reverseSoftLimitEnable = true;
    steerMotor.getConfigurator().apply(steerConfig);

    enabled = true;
  }

  public void setDesiredState(double speedMps, double angleRad) {
    targetSpeedMps = Math.max(-4.5, Math.min(4.5, speedMps));
    targetAngleRad = angleRad;
  }

  public void periodic() {
    if (!enabled) {
      driveMotor.setNeutralOutput();
      steerMotor.setNeutralOutput();
      return;
    }

    double currentAngleRad = getSteerAngle();
    double steerOutput = steerPID.calculate(currentAngleRad, targetAngleRad);
    steerOutput = Math.max(-1.0, Math.min(1.0, steerOutput));
    steerMotor.setDutyCycle(steerOutput);

    double currentSpeed = getDriveVelocity();
    double driveOutput = drivePID.calculate(currentSpeed, targetSpeedMps);
    driveOutput = Math.max(-1.0, Math.min(1.0, driveOutput));
    driveMotor.setDutyCycle(driveOutput);
  }

  public double getSteerAngle() {
    double rotations = encoder.getPosition().getValue().in(Rotations);
    return rotations * 2.0 * Math.PI;
  }

  public double getDriveVelocity() {
    double rotPerSec = driveMotor.getVelocity().getValue().in(RotationsPerSecond);
    return rotPerSec * WHEEL_CIRCUMFERENCE_METERS;
  }

  public Translation2d getModuleVelocity() {
    double speed = getDriveVelocity();
    double angle = getSteerAngle();
    return new Translation2d(
        speed * Math.cos(angle),
        speed * Math.sin(angle));
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
    if (!enabled) {
      driveMotor.setNeutralOutput();
      steerMotor.setNeutralOutput();
    }
  }

  public boolean isEnabled() {
    return enabled;
  }

  public String getLabel() {
    return label;
  }
}