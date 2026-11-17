package frc.robot.Vision;

import dev.doglog.DogLog;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.RobotBase;
import edu.wpi.first.wpilibj.Timer;
import org.photonvision.EstimatedRobotPose;
import org.photonvision.PhotonCamera;
import org.photonvision.PhotonPoseEstimator;
import org.photonvision.simulation.PhotonCameraSim;
import org.photonvision.simulation.SimCameraProperties;
import org.photonvision.simulation.VisionSystemSim;
import org.photonvision.targeting.PhotonPipelineResult;
import org.photonvision.targeting.PhotonTrackedTarget;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * PhotonVision facade for the two front RubikPi cameras.
 *
 * <p>Real-robot vision is disabled until both camera transforms are measured and
 * {@link VisionConstants#CAMERA_TRANSFORMS_CONFIGURED} is set to true.
 */
public class Vision {
    public record Measurement(
            Pose2d pose,
            double timestampSeconds,
            double avgDistanceMeters,
            double maxAmbiguity,
            double linearStandardDeviationMeters) {
        /** Vision never controls heading; Pigeon 2 is authoritative. */
        public static final double ANGULAR_STD_DEV = 1e6;
    }

    private static final class CameraBundle {
        final String name;
        final Transform3d robotToCamera;
        final PhotonCamera camera;
        final PhotonPoseEstimator estimator;
        PhotonCameraSim cameraSim;

        CameraBundle(String name, Transform3d robotToCamera) {
            this.name = name;
            this.robotToCamera = robotToCamera;
            this.camera = new PhotonCamera(name);
            this.estimator = new PhotonPoseEstimator(VisionConstants.FIELD_LAYOUT, robotToCamera);
        }
    }

    private final List<CameraBundle> cameras = new ArrayList<>();
    private final List<Measurement> pending = new ArrayList<>();
    private final boolean simulation;
    private final VisionSystemSim visionSim;

    private boolean enabled = true;
    private boolean warnedTransformsMissing = false;
    private double latestAcceptedTimestamp = -1.0;
    private Optional<Pose2d> latestAcceptedPose = Optional.empty();
    private int lastAcceptedCount = 0;

    public Vision() {
        simulation = RobotBase.isSimulation();
        cameras.add(new CameraBundle(
                VisionConstants.FRONT_LEFT_CAMERA, VisionConstants.ROBOT_TO_FRONT_LEFT));
        cameras.add(new CameraBundle(
                VisionConstants.FRONT_RIGHT_CAMERA, VisionConstants.ROBOT_TO_FRONT_RIGHT));

        if (simulation) {
            visionSim = new VisionSystemSim("photonSim");
            visionSim.addAprilTags(VisionConstants.FIELD_LAYOUT);
            SimCameraProperties properties = new SimCameraProperties();
            properties.setCalibration(
                    VisionConstants.SIM_WIDTH,
                    VisionConstants.SIM_HEIGHT,
                    Rotation2d.fromDegrees(VisionConstants.SIM_FOV_DEG));
            properties.setCalibError(0.25, 0.08);
            properties.setFPS(VisionConstants.SIM_FPS);
            properties.setAvgLatencyMs(VisionConstants.SIM_AVG_LATENCY_MS);
            properties.setLatencyStdDevMs(VisionConstants.SIM_LATENCY_STD_DEV_MS);

            for (CameraBundle bundle : cameras) {
                bundle.cameraSim = new PhotonCameraSim(bundle.camera, properties);
                visionSim.addCamera(bundle.cameraSim, bundle.robotToCamera);
            }
        } else {
            visionSim = null;
        }
    }

    public void periodic(Pose2d referencePose) {
        if (simulation) {
            return;
        }
        if (!areTransformsConfigured()) {
            if (!warnedTransformsMissing) {
                warnedTransformsMissing = true;
                DriverStation.reportWarning(
                        "Vision disabled: measure the front-left/front-right camera transforms "
                                + "and set VisionConstants.CAMERA_TRANSFORMS_CONFIGURED = true.",
                        false);
            }
            return;
        }
        if (!enabled) {
            return;
        }
        processAllCameras(referencePose);
    }

    public List<Measurement> drainMeasurements(Pose2d referencePose) {
        if (simulation) {
            visionSim.update(new Pose3d(referencePose));
            if (enabled) {
                processAllCameras(referencePose);
            }
        }
        List<Measurement> output = List.copyOf(pending);
        pending.clear();
        return output;
    }

    public Optional<Pose2d> getLatestAcceptedPose() {
        return latestAcceptedPose;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean areTransformsConfigured() {
        return simulation || VisionConstants.CAMERA_TRANSFORMS_CONFIGURED;
    }

    public boolean isVisionActive() {
        return areTransformsConfigured()
                && enabled
                && latestAcceptedTimestamp > 0.0
                && Timer.getFPGATimestamp() - latestAcceptedTimestamp
                        <= VisionConstants.MAX_MEASUREMENT_AGE_SECONDS;
    }

    public int getLastAcceptedCount() {
        return lastAcceptedCount;
    }

    public boolean isFrontLeftConnected() {
        return cameras.get(0).camera.isConnected();
    }

    public boolean isFrontRightConnected() {
        return cameras.get(1).camera.isConnected();
    }

    private void processAllCameras(Pose2d referencePose) {
        int accepted = 0;
        for (CameraBundle bundle : cameras) {
            accepted += processCamera(bundle, referencePose);
        }
        lastAcceptedCount = accepted;
    }

    private int processCamera(CameraBundle bundle, Pose2d referencePose) {
        List<PhotonPipelineResult> results = bundle.camera.getAllUnreadResults();
        if (results == null || results.isEmpty()) {
            DogLog.log("Vision/" + bundle.name + "/Connected", bundle.camera.isConnected());
            DogLog.log("Vision/" + bundle.name + "/HasTargets", false);
            return 0;
        }

        int accepted = 0;
        boolean anyTargets = false;
        for (PhotonPipelineResult result : results) {
            anyTargets |= result.hasTargets();
            Optional<EstimatedRobotPose> estimate = bundle.estimator.estimateCoprocMultiTagPose(result);
            boolean multiTag = estimate.isPresent();
            if (estimate.isEmpty()) {
                estimate = bundle.estimator.estimateLowestAmbiguityPose(result);
            }
            if (estimate.isEmpty()) {
                continue;
            }

            EstimatedRobotPose robotPose = estimate.get();
            if (!VisionMeasurementFilter.isPoseHeightValid(robotPose.estimatedPose)) {
                continue;
            }

            Pose2d fieldPose = robotPose.estimatedPose.toPose2d();
            if (!VisionMeasurementFilter.isPoseInField(
                    fieldPose,
                    VisionConstants.FIELD_LAYOUT,
                    VisionConstants.FIELD_MARGIN_METERS)) {
                continue;
            }

            List<PhotonTrackedTarget> targets = robotPose.targetsUsed;
            if (targets == null || targets.isEmpty()) {
                continue;
            }

            double avgDistance = averageDistanceMeters(targets);
            double maxAmbiguity = maxAmbiguity(targets);
            if (avgDistance > VisionConstants.MAX_DISTANCE_METERS) {
                continue;
            }
            if (!multiTag
                    && !VisionMeasurementFilter.shouldAcceptSingleTag(maxAmbiguity, avgDistance)) {
                continue;
            }

            double timestamp = simulation ? Timer.getFPGATimestamp() : robotPose.timestampSeconds;
            if (!simulation
                    && Timer.getFPGATimestamp() - timestamp
                            > VisionConstants.MAX_MEASUREMENT_AGE_SECONDS) {
                continue;
            }

            pending.add(new Measurement(
                    fieldPose,
                    timestamp,
                    avgDistance,
                    maxAmbiguity,
                    VisionMeasurementFilter.linearStdDev(avgDistance)));
            latestAcceptedPose = Optional.of(fieldPose);
            latestAcceptedTimestamp = timestamp;
            accepted++;

            DogLog.log("Vision/" + bundle.name + "/LastPoseX", fieldPose.getX());
            DogLog.log("Vision/" + bundle.name + "/LastPoseY", fieldPose.getY());
            DogLog.log("Vision/" + bundle.name + "/LastDistance", avgDistance);
            DogLog.log("Vision/" + bundle.name + "/LastAmbiguity", maxAmbiguity);
            DogLog.log("Vision/" + bundle.name + "/MultiTag", multiTag);
        }

        DogLog.log("Vision/" + bundle.name + "/Connected", bundle.camera.isConnected());
        DogLog.log("Vision/" + bundle.name + "/HasTargets", anyTargets);
        DogLog.log("Vision/" + bundle.name + "/Accepted", accepted);
        return accepted;
    }

    private static double averageDistanceMeters(List<PhotonTrackedTarget> targets) {
        double sum = 0.0;
        for (PhotonTrackedTarget target : targets) {
            sum += target.getBestCameraToTarget().getTranslation().getNorm();
        }
        return sum / targets.size();
    }

    private static double maxAmbiguity(List<PhotonTrackedTarget> targets) {
        double max = -1.0;
        for (PhotonTrackedTarget target : targets) {
            max = Math.max(max, target.getPoseAmbiguity());
        }
        return max;
    }
}
