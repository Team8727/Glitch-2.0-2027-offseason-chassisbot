package frc.robot.vision;

import java.util.List;
import java.util.Optional;
import org.photonvision.EstimatedRobotPose;
import org.photonvision.PhotonCamera;
import org.photonvision.PhotonPoseEstimator;
import org.photonvision.PhotonPoseEstimator.PoseStrategy;
import org.photonvision.targeting.PhotonPipelineResult;
import org.wpilib.math.geometry.Pose2d;
import org.wpilib.math.geometry.Transform3d;

/**
 * Two-camera PhotonVision front end.
 *
 * <p>UNVERIFIED against PhotonLib 2027 alpha: the package names are expected to
 * stay {@code org.photonvision}, but the WPILib geometry types moved to
 * {@code org.wpilib.math}. Install the 2027 PhotonLib vendordep and compile
 * before trusting this class.
 */
public class Vision {
    private final PhotonCamera frontLeft;
    private final PhotonCamera frontRight;
    private final PhotonPoseEstimator frontLeftEstimator;
    private final PhotonPoseEstimator frontRightEstimator;

    private int lastAcceptedCount = 0;
    private double lastAcceptedTimestamp = -1.0;
    private Pose2d lastAcceptedPose = null;

    public Vision(org.wpilib.apriltag.AprilTagFieldLayout layout,
            Transform3d frontLeftTransform, Transform3d frontRightTransform) {
        frontLeft = new PhotonCamera("FrontL");
        frontRight = new PhotonCamera("FrontR");
        frontLeftEstimator = new PhotonPoseEstimator(
                layout, PoseStrategy.MULTI_TAG_PNP_ON_COPROCESSOR, frontLeftTransform);
        frontRightEstimator = new PhotonPoseEstimator(
                layout, PoseStrategy.MULTI_TAG_PNP_ON_COPROCESSOR, frontRightTransform);
        frontLeftEstimator.setMultiTagFallbackStrategy(PoseStrategy.CLOSEST_TO_REFERENCE_POSE);
        frontRightEstimator.setMultiTagFallbackStrategy(PoseStrategy.CLOSEST_TO_REFERENCE_POSE);
    }

    public void update(Pose2d referencePose, DriveSink sink) {
        lastAcceptedCount = 0;
        lastAcceptedCount += process(frontLeft, frontLeftEstimator, referencePose, sink);
        lastAcceptedCount += process(frontRight, frontRightEstimator, referencePose, sink);
    }

    private int process(PhotonCamera camera, PhotonPoseEstimator estimator, Pose2d referencePose, DriveSink sink) {
        if (referencePose != null) {
            estimator.setReferencePose(referencePose);
        }

        List<PhotonPipelineResult> results = camera.getAllUnreadResults();
        if (results == null) {
            return 0;
        }

        int accepted = 0;
        for (PhotonPipelineResult result : results) {
            if (!result.hasTargets()) {
                continue;
            }
            if (!isGood(result)) {
                continue;
            }
            Optional<EstimatedRobotPose> estimate = estimator.estimateCoprocMultiTagPose(result);
            if (estimate.isEmpty()) {
                estimate = estimator.estimateLowestAmbiguityPose(result);
            }
            if (estimate.isEmpty()) {
                continue;
            }

            EstimatedRobotPose pose = estimate.get();
            sink.addVisionMeasurement(
                    pose.estimatedPose.toPose2d(),
                    pose.timestampSeconds,
                    stdDevs(result.getTargets().size(), averageDistance(result)));
            lastAcceptedPose = pose.estimatedPose.toPose2d();
            lastAcceptedTimestamp = pose.timestampSeconds;
            accepted++;
        }
        return accepted;
    }

    private static boolean isGood(PhotonPipelineResult result) {
        if (result.getTargets().size() >= 2) {
            return true;
        }
        return result.getBestTarget() != null && result.getBestTarget().getPoseAmbiguity() < 0.2;
    }

    private static double averageDistance(PhotonPipelineResult result) {
        return result.getTargets().stream()
                .mapToDouble(t -> t.getBestCameraToTarget().getTranslation().getNorm())
                .average()
                .orElse(99.0);
    }

    private static org.wpilib.math.Matrix<org.wpilib.math.numbers.N3, org.wpilib.math.numbers.N1> stdDevs(
            int tags, double distanceMeters) {
        double xy = Math.max(0.2, 0.03 * distanceMeters * distanceMeters / Math.max(1, tags));
        double theta = Math.max(1.0, xy * 20.0);
        return org.wpilib.math.MatBuilder.fill(xy, xy, theta);
    }

    public int getLastAcceptedCount() {
        return lastAcceptedCount;
    }

    public boolean isActive() {
        return lastAcceptedPose != null && lastAcceptedTimestamp > 0.0;
    }

    public interface DriveSink {
        void addVisionMeasurement(Pose2d pose, double timestampSeconds,
                org.wpilib.math.Matrix<org.wpilib.math.numbers.N3, org.wpilib.math.numbers.N1> stdDevs);
    }
}
