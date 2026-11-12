package frc.robot.Vision;

import static frc.robot.Constants.*;

import java.util.Optional;

import org.photonvision.EstimatedRobotPose;
import org.photonvision.PhotonCamera;
import org.photonvision.PhotonPoseEstimator;
import org.photonvision.PhotonPoseEstimator.PoseStrategy;

import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.apriltag.AprilTagFields;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class VisionSubsystem extends SubsystemBase {
    private final PhotonCamera frontLeftCamera;
    private final PhotonCamera frontRightCamera;
    private final PhotonPoseEstimator frontLeftEstimator;
    private final PhotonPoseEstimator frontRightEstimator;
    private final AprilTagFieldLayout fieldLayout;

    private EstimatedRobotPose lastFrontLeftPose = null;
    private EstimatedRobotPose lastFrontRightPose = null;

    // Robot-space transforms for cameras (placeholders — measure actual values)
    private static final Transform3d FRONT_LEFT_TRANSFORM = new Transform3d();
    private static final Transform3d FRONT_RIGHT_TRANSFORM = new Transform3d();

    public VisionSubsystem() {
        frontLeftCamera = new PhotonCamera(CAMERA_FRONT_LEFT_NAME);
        frontRightCamera = new PhotonCamera(CAMERA_FRONT_RIGHT_NAME);

        fieldLayout = AprilTagFieldLayout.loadField(AprilTagFields.k2026_Rebuilt);
        // Note: for 2027 game, swap to the appropriate field layout after reveal

        frontLeftEstimator = new PhotonPoseEstimator(
            fieldLayout, PoseStrategy.MULTI_TAG_PNP_ON_COPROCESSOR,
            frontLeftCamera, FRONT_LEFT_TRANSFORM);

        frontRightEstimator = new PhotonPoseEstimator(
            fieldLayout, PoseStrategy.MULTI_TAG_PNP_ON_COPROCESSOR,
            frontRightCamera, FRONT_RIGHT_TRANSFORM);
    }

    public Optional<EstimatedRobotPose> getFrontLeftPose() {
        if (lastFrontLeftPose == null) return Optional.empty();
        return Optional.of(lastFrontLeftPose);
    }

    public Optional<EstimatedRobotPose> getFrontRightPose() {
        if (lastFrontRightPose == null) return Optional.empty();
        return Optional.of(lastFrontRightPose);
    }

    public boolean hasTargets() {
        return frontLeftCamera.getLatestResult().hasTargets()
            || frontRightCamera.getLatestResult().hasTargets();
    }

    @Override
    public void periodic() {
        if (!ENABLE_VISION) return;

        var leftResult = frontLeftCamera.getLatestResult();
        if (leftResult.hasTargets()) {
            Optional<EstimatedRobotPose> pose = frontLeftEstimator.update(leftResult);
            if (pose.isPresent()) {
                lastFrontLeftPose = pose.get();
                SmartDashboard.putNumber("vision/frontLeft/tagCount", leftResult.targets.size());
                SmartDashboard.putNumber("vision/frontLeft/latencyMs", leftResult.getLatencyMillis());
            }
        }

        var rightResult = frontRightCamera.getLatestResult();
        if (rightResult.hasTargets()) {
            Optional<EstimatedRobotPose> pose = frontRightEstimator.update(rightResult);
            if (pose.isPresent()) {
                lastFrontRightPose = pose.get();
                SmartDashboard.putNumber("vision/frontRight/tagCount", rightResult.targets.size());
                SmartDashboard.putNumber("vision/frontRight/latencyMs", rightResult.getLatencyMillis());
            }
        }

        SmartDashboard.putBoolean("vision/hasTargets", hasTargets());
    }
}