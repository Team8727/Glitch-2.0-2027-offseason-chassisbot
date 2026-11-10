package frc.robot.Drivetrain;

import com.ctre.phoenix6.swerve.SwerveDrivetrain.SwerveDriveState;
import dev.doglog.DogLog;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.networktables.BooleanPublisher;
import edu.wpi.first.networktables.DoublePublisher;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.StringPublisher;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
 * Publishes swerve state to DogLog and to a stable {@code /Elastic} NT4 table.
 *
 * <p>Elastic widgets read the explicit {@code /Elastic/...} topics. Publishing
 * to one predictable table avoids the duplicate/stale topics that caused the
 * connection problems on the 2026 dashboard.
 */
public class DrivetrainTelemetry {
    private final Field2d field2d = new Field2d();
    private final NetworkTable elastic =
            NetworkTableInstance.getDefault().getTable("Elastic");

    private final DoublePublisher poseX = elastic.getDoubleTopic("Drive/PoseX").publish();
    private final DoublePublisher poseY = elastic.getDoubleTopic("Drive/PoseY").publish();
    private final DoublePublisher headingDeg =
            elastic.getDoubleTopic("Drive/HeadingDeg").publish();
    private final DoublePublisher speedMps =
            elastic.getDoubleTopic("Drive/SpeedMps").publish();
    private final DoublePublisher omegaDegPerSec =
            elastic.getDoubleTopic("Drive/OmegaDegPerSec").publish();
    private final DoublePublisher odometryHz =
            elastic.getDoubleTopic("Drive/OdometryHz").publish();
    private final DoublePublisher batteryVolts =
            elastic.getDoubleTopic("Robot/BatteryVolts").publish();
    private final DoublePublisher loopTimeMs =
            elastic.getDoubleTopic("Robot/LoopTimeMs").publish();
    private final BooleanPublisher visionActive =
            elastic.getBooleanTopic("Vision/Active").publish();
    private final DoublePublisher visionAccepted =
            elastic.getDoubleTopic("Vision/AcceptedMeasurements").publish();
    private final BooleanPublisher frontLeftConnected =
            elastic.getBooleanTopic("Vision/FrontLeftConnected").publish();
    private final BooleanPublisher frontRightConnected =
            elastic.getBooleanTopic("Vision/FrontRightConnected").publish();
    private final StringPublisher ledState =
            elastic.getStringTopic("LED/State").publish();

    public DrivetrainTelemetry(CTRESwerveDrivetrain drivetrain) {
        SmartDashboard.putData("Field", field2d);
        drivetrain.registerTelemetry(this::telemeterize);
    }

    public Field2d getField2d() {
        return field2d;
    }

    public void telemeterize(SwerveDriveState state) {
        field2d.setRobotPose(state.Pose);

        poseX.set(state.Pose.getX());
        poseY.set(state.Pose.getY());
        headingDeg.set(state.Pose.getRotation().getDegrees());
        speedMps.set(Math.hypot(state.Speeds.vxMetersPerSecond, state.Speeds.vyMetersPerSecond));
        omegaDegPerSec.set(Math.toDegrees(state.Speeds.omegaRadiansPerSecond));
        odometryHz.set(1.0 / Math.max(state.OdometryPeriod, 1e-6));

        DogLog.log("Drive/PoseX", state.Pose.getX());
        DogLog.log("Drive/PoseY", state.Pose.getY());
        DogLog.log("Drive/PoseHeadingDeg", state.Pose.getRotation().getDegrees());
        DogLog.log("Drive/VxMetersPerSecond", state.Speeds.vxMetersPerSecond);
        DogLog.log("Drive/VyMetersPerSecond", state.Speeds.vyMetersPerSecond);
        DogLog.log("Drive/OmegaRadiansPerSecond", state.Speeds.omegaRadiansPerSecond);
        DogLog.log("Drive/OdometryHz", 1.0 / Math.max(state.OdometryPeriod, 1e-6));

        SwerveModuleState[] states = state.ModuleStates;
        SwerveModuleState[] targets = state.ModuleTargets;
        for (int i = 0; i < states.length; i++) {
            DogLog.log("Drive/Module" + i + "/AngleDeg", states[i].angle.getDegrees());
            DogLog.log("Drive/Module" + i + "/SpeedMetersPerSecond", states[i].speedMetersPerSecond);
            DogLog.log("Drive/Module" + i + "/TargetAngleDeg", targets[i].angle.getDegrees());
            DogLog.log("Drive/Module" + i + "/TargetSpeedMetersPerSecond", targets[i].speedMetersPerSecond);
        }
    }

    public void publishHealth(
            double battery,
            double loopMs,
            boolean visionIsActive,
            int acceptedMeasurements,
            boolean frontLeftOk,
            boolean frontRightOk,
            String ledStateName) {
        batteryVolts.set(battery);
        loopTimeMs.set(loopMs);
        visionActive.set(visionIsActive);
        visionAccepted.set(acceptedMeasurements);
        frontLeftConnected.set(frontLeftOk);
        frontRightConnected.set(frontRightOk);
        ledState.set(ledStateName);
    }
}
