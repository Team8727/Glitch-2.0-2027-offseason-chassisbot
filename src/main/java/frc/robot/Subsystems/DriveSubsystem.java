package frc.robot.Subsystems;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class DriveSubsystem extends SubsystemBase {
  public static final double TRACK_WIDTH_METERS = 0.5969;
  public static final double WHEEL_BASE_METERS = 0.5969;
  public static final double MAX_SPEED_MPS = 4.5;
  public static final double MAX_ANGULAR_SPEED_RPS = Math.PI * 2;

  private static DriveSubsystem instance;

  private final SwerveModule frontLeft;
  private final SwerveModule frontRight;
  private final SwerveModule backLeft;
  private final SwerveModule backRight;
  private final SwerveDriveKinematics kinematics;

  private Pose2d robotPose;
  private boolean enabled;
  private boolean autoMode;
  private double lastTimestamp;

  private DriveSubsystem() {
    frontLeft = new SwerveModule(11, 12, 13, "FL");
    frontRight = new SwerveModule(14, 15, 16, "FR");
    backLeft = new SwerveModule(17, 18, 19, "BL");
    backRight = new SwerveModule(20, 21, 22, "BR");

    robotPose = new Pose2d();
    Translation2d[] modulePositions = {
        new Translation2d(WHEEL_BASE_METERS / 2.0, TRACK_WIDTH_METERS / 2.0),
        new Translation2d(WHEEL_BASE_METERS / 2.0, -TRACK_WIDTH_METERS / 2.0),
        new Translation2d(-WHEEL_BASE_METERS / 2.0, TRACK_WIDTH_METERS / 2.0),
        new Translation2d(-WHEEL_BASE_METERS / 2.0, -TRACK_WIDTH_METERS / 2.0)
    };
    kinematics = new SwerveDriveKinematics(modulePositions);
    enabled = true;
    autoMode = false;
    lastTimestamp = Timer.getFPGATimestamp();
  }

  public static DriveSubsystem getInstance() {
    if (instance == null) {
      instance = new DriveSubsystem();
    }
    return instance;
  }

  public void setAutoMode(boolean autoMode) {
    this.autoMode = autoMode;
  }

  public boolean isAutoMode() {
    return autoMode;
  }

  public void resetOdometry() {
    robotPose = new Pose2d();
  }

  public void setDesiredChassisSpeeds(double vxMps, double vyMps, double omegaRps) {
    if (!enabled) {
      stop();
      return;
    }

    double vxClamped = Math.max(-MAX_SPEED_MPS, Math.min(MAX_SPEED_MPS, vxMps));
    double vyClamped = Math.max(-MAX_SPEED_MPS, Math.min(MAX_SPEED_MPS, vyMps));
    double omegaClamped = Math.max(
        -MAX_ANGULAR_SPEED_RPS, Math.min(MAX_ANGULAR_SPEED_RPS, omegaRps));

    double halfTrack = TRACK_WIDTH_METERS / 2.0;
    double halfBase = WHEEL_BASE_METERS / 2.0;

    double a = vxClamped - omegaClamped * halfTrack;
    double b = vxClamped + omegaClamped * halfTrack;
    double c = vyClamped - omegaClamped * halfBase;
    double d = vyClamped + omegaClamped * halfBase;

    frontLeft.setDesiredState(Math.hypot(a, d), Math.atan2(d, a));
    frontRight.setDesiredState(Math.hypot(b, d), Math.atan2(d, b));
    backLeft.setDesiredState(Math.hypot(a, c), Math.atan2(c, a));
    backRight.setDesiredState(Math.hypot(b, c), Math.atan2(c, b));
  }

  public void setModuleStates(double[] speedsMps, double[] anglesRad) {
    if (speedsMps.length >= 4 && anglesRad.length >= 4) {
      frontLeft.setDesiredState(speedsMps[0], anglesRad[0]);
      frontRight.setDesiredState(speedsMps[1], anglesRad[1]);
      backLeft.setDesiredState(speedsMps[2], anglesRad[2]);
      backRight.setDesiredState(speedsMps[3], anglesRad[3]);
    }
  }

  public Pose2d getPose() {
    return robotPose;
  }

  public SwerveDriveKinematics getKinematics() {
    return kinematics;
  }

  public ChassisSpeeds getFieldSpeeds() {
    Translation2d flVel = frontLeft.getModuleVelocity();
    Translation2d frVel = frontRight.getModuleVelocity();
    Translation2d blVel = backLeft.getModuleVelocity();
    Translation2d brVel = backRight.getModuleVelocity();

    double avgVx = (flVel.getX() + frVel.getX() + blVel.getX() + brVel.getX()) / 4.0;
    double avgVy = (flVel.getY() + frVel.getY() + blVel.getY() + brVel.getY()) / 4.0;

    double cos = Math.cos(robotPose.getRotation().getRadians());
    double sin = Math.sin(robotPose.getRotation().getRadians());
    double fieldVx = avgVx * cos - avgVy * sin;
    double fieldVy = avgVx * sin + avgVy * cos;

    return new ChassisSpeeds(fieldVx, fieldVy, 0.0);
  }

  public double getMaxSpeed() {
    return MAX_SPEED_MPS;
  }

  public double getMaxAngularSpeed() {
    return MAX_ANGULAR_SPEED_RPS;
  }

  @Override
  public void periodic() {
    if (!enabled) {
      return;
    }

    double now = Timer.getFPGATimestamp();
    double dt = now - lastTimestamp;
    lastTimestamp = now;
    dt = Math.max(0.001, Math.min(dt, 0.1));

    Translation2d flVel = frontLeft.getModuleVelocity();
    Translation2d frVel = frontRight.getModuleVelocity();
    Translation2d blVel = backLeft.getModuleVelocity();
    Translation2d brVel = backRight.getModuleVelocity();

    double avgVx = (flVel.getX() + frVel.getX() + blVel.getX() + brVel.getX()) / 4.0;
    double avgVy = (flVel.getY() + frVel.getY() + blVel.getY() + brVel.getY()) / 4.0;

    double cos = Math.cos(robotPose.getRotation().getRadians());
    double sin = Math.sin(robotPose.getRotation().getRadians());
    double fieldVx = avgVx * cos - avgVy * sin;
    double fieldVy = avgVx * sin + avgVy * cos;

    robotPose = new Pose2d(
        robotPose.getX() + fieldVx * dt,
        robotPose.getY() + fieldVy * dt,
        robotPose.getRotation());

    frontLeft.periodic();
    frontRight.periodic();
    backLeft.periodic();
    backRight.periodic();
  }

  public void stop() {
    frontLeft.setDesiredState(0.0, 0.0);
    frontRight.setDesiredState(0.0, 0.0);
    backLeft.setDesiredState(0.0, 0.0);
    backRight.setDesiredState(0.0, 0.0);
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
    if (!enabled) {
      stop();
    }
    frontLeft.setEnabled(enabled);
    frontRight.setEnabled(enabled);
    backLeft.setEnabled(enabled);
    backRight.setEnabled(enabled);
  }

  public boolean isEnabled() {
    return enabled;
  }
}