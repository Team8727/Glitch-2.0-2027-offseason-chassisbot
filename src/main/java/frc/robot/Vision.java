package frc.robot;

import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.apriltag.AprilTagFields;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.networktables.DoubleArrayPublisher;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.RobotBase;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
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
 * Robot-layer Vision configuration and facade.
 *
 * <p>Owns the AprilTag field layout, both PhotonVision cameras (FrontLeft /
 * FrontRight on the Rubik Pi coprocessor), their robot-to-camera transforms, and
 * the quality filters applied before measurements are fused into the CTRE pose
 * estimator by {@code CTRESwerveDrivetrain}. Produces a real provider backed by
 * PhotonVision on the Rubik Pi, and an in-code simulated provider when running
 * in simulation.
 */
public class Vision implements AutoCloseable {

  /**
   * Robot-layer measurement wrapper decoupling robot code from PhotonLib types.
   *
   * @param pose Estimated field pose at capture time.
   * @param timestampSeconds Capture timestamp in seconds.
   */
  public record Measurement(Pose2d pose, double timestampSeconds) {}

  private static final String CAM_FRONT_RIGHT = "FrontRight";
  private static final String CAM_FRONT_LEFT = "FrontLeft";

  // VERIFY (bench): camera names must match the PhotonVision dashboard on the
  // Rubik Pi, and these transforms must match how the cameras are physically mounted.
  private static final Transform3d FRONT_RIGHT =
    new Transform3d(
        new Translation3d(
            Units.inchesToMeters(2.375),
            Units.inchesToMeters(-2.875),
            Units.inchesToMeters(28)),
        new Rotation3d(0, 0, Math.toRadians(-10))
    );
  private static final Transform3d FRONT_LEFT =
    new Transform3d(
        new Translation3d(
            Units.inchesToMeters(2.375),
            Units.inchesToMeters(3.25),
            Units.inchesToMeters(28)),
        new Rotation3d(0, 0, Math.toRadians(10))
    );

  // Quality filters (VERIFY: tune after first hardware runs)
  private static final double MAX_AMBIGUITY = 0.2; // ignore -1 (handled below)
  private static final double MAX_DISTANCE_METERS = 3.5;

  // Simulation properties
  private static final int SIM_WIDTH = 640;
  private static final int SIM_HEIGHT = 480;
  private static final double SIM_FOV_DEG = 70; // VERIFY: swap if using 120-deg lenses
  private static final int SIM_FPS = 40;
  private static final double SIM_AVG_LATENCY_MS = 35;

  private final AprilTagFieldLayout layout;
  private final PhotonCamera frontLeftCamera = new PhotonCamera(CAM_FRONT_LEFT);
  private final PhotonCamera frontRightCamera = new PhotonCamera(CAM_FRONT_RIGHT);
  private final PhotonPoseEstimator frontLeftEstimator;
  private final PhotonPoseEstimator frontRightEstimator;
  private final VisionSystemSim visionSim;
  private final Field2d visionField = new Field2d();
  private final DoubleArrayPublisher[] cameraPosePublishers = new DoubleArrayPublisher[2];

  /**
   * Constructs the Vision facade and its per-camera pose estimators.
   */
  public Vision() {
    layout = AprilTagFieldLayout.loadField(AprilTagFields.k2026RebuiltAndymark);

    // PhotonLib v2026 estimators use the 2-arg constructor (field layout +
    // robot-to-camera transform) plus per-strategy estimate methods.
    frontLeftEstimator = new PhotonPoseEstimator(layout, FRONT_LEFT);
    frontRightEstimator = new PhotonPoseEstimator(layout, FRONT_RIGHT);

    SmartDashboard.putData("Vision Field", visionField);
    var nt = NetworkTableInstance.getDefault();
    cameraPosePublishers[0] = nt.getDoubleArrayTopic("/Vision/" + CAM_FRONT_LEFT + "/Pose").publish();
    cameraPosePublishers[1] = nt.getDoubleArrayTopic("/Vision/" + CAM_FRONT_RIGHT + "/Pose").publish();

    if (RobotBase.isSimulation()) {
      visionSim = new VisionSystemSim("main");
      visionSim.addAprilTags(layout);
      addSimCamera(CAM_FRONT_LEFT, frontLeftCamera, FRONT_LEFT);
      addSimCamera(CAM_FRONT_RIGHT, frontRightCamera, FRONT_RIGHT);
    } else {
      visionSim = null;
    }
  }

  private void addSimCamera(String name, PhotonCamera camera, Transform3d robotToCamera) {
    SimCameraProperties properties = new SimCameraProperties();
    properties.setCalibration(SIM_WIDTH, SIM_HEIGHT, Rotation2d.fromDegrees(SIM_FOV_DEG));
    properties.setCalibError(0.5, 0.08);
    properties.setFPS(SIM_FPS);
    properties.setAvgLatencyMs(SIM_AVG_LATENCY_MS);

    PhotonCameraSim cameraSim = new PhotonCameraSim(camera, properties);
    cameraSim.setMaxSightRange(MAX_DISTANCE_METERS);
    visionSim.addCamera(cameraSim, robotToCamera);
  }

  /**
   * Per-loop provider hook. In simulation this drives the virtual cameras from
   * the supplied robot pose; in production this is a no-op.
   *
   * @param robotPose Current best estimate of robot pose.
   */
  public void periodic(Pose2d robotPose) {
    if (visionSim != null) {
      visionSim.update(robotPose);
    }
    logCameraPoses(robotPose);
  }

  /**
   * Retrieves and maps any available measurements from both cameras using the
   * supplied reference pose for disambiguation.
   *
   * @param referencePose Current best estimate of robot pose.
   * @return Robot-layer measurements (pose, timestamp).
   */
  public List<Measurement> drainMeasurements(Pose2d referencePose) {
    List<Measurement> measurements = new ArrayList<>();
    estimatePose(frontLeftCamera, frontLeftEstimator, referencePose)
        .ifPresent(m -> measurements.add(new Measurement(m.pose, m.timestampSeconds)));
    estimatePose(frontRightCamera, frontRightEstimator, referencePose)
        .ifPresent(m -> measurements.add(new Measurement(m.pose, m.timestampSeconds)));
    return measurements;
  }

  /**
   * Attempts to compute an initial robot pose from visible tags (e.g., at boot).
   *
   * @return Optional initial field pose if a valid target is currently visible.
   */
  public Optional<Pose2d> bestStartPose() {
    Pose2d origin = new Pose2d();
    Optional<Measurement> left = estimatePose(frontLeftCamera, frontLeftEstimator, origin);
    if (left.isPresent()) {
      return Optional.of(left.get().pose);
    }
    return estimatePose(frontRightCamera, frontRightEstimator, origin)
        .map(m -> m.pose());
  }

  private Optional<Measurement> estimatePose(
      PhotonCamera camera,
      PhotonPoseEstimator estimator,
      Pose2d referencePose) {
    // Process all unread pipeline results; keep the newest accepted estimate.
    Optional<EstimatedRobotPose> est = Optional.empty();
    for (PhotonPipelineResult result : camera.getAllUnreadResults()) {
      if (!result.hasTargets()) {
        continue;
      }
      // Prefer on-coprocessor multi-tag (computed on the Rubik Pi), falling back
      // to lowest-ambiguity single-tag on the RIO.
      Optional<EstimatedRobotPose> candidate = estimator.estimateCoprocMultiTagPose(result);
      if (candidate.isEmpty()) {
        candidate = estimator.estimateLowestAmbiguityPose(result);
      }
      if (candidate.isPresent()
          && (est.isEmpty() || candidate.get().timestampSeconds > est.get().timestampSeconds)) {
        est = candidate;
      }
    }
    if (est.isEmpty()) {
      return Optional.empty();
    }

    EstimatedRobotPose robotPose = est.get();
    // Distance / ambiguity filter: reject untrustworthy estimates.
    for (PhotonTrackedTarget target : robotPose.targetsUsed) {
      double ambiguity = target.getPoseAmbiguity();
      double distance = target.getBestCameraToTarget().getTranslation().getNorm();
      if (ambiguity != -1 && ambiguity > MAX_AMBIGUITY) {
        return Optional.empty();
      }
      if (distance > MAX_DISTANCE_METERS) {
        return Optional.empty();
      }
    }

    Pose2d pose = robotPose.estimatedPose.toPose2d();
    visionField.setRobotPose(pose);
    return Optional.of(new Measurement(pose, robotPose.timestampSeconds));
  }

  /**
   * Publishes each camera's current field pose to NetworkTables for visualization.
   *
   * @param robotPose Current robot field pose (fused or odometry).
   */
  public void logCameraPoses(Pose2d robotPose) {
    Pose3d robotPose3d = new Pose3d(robotPose);
    logCameraPose(0, robotPose3d.transformBy(FRONT_LEFT));
    logCameraPose(1, robotPose3d.transformBy(FRONT_RIGHT));
  }

  private void logCameraPose(int index, Pose3d pose) {
    double[] arr = {pose.getX(), pose.getY(), pose.getRotation().getZ()};
    cameraPosePublishers[index].set(arr);
  }

  @Override
  public void close() {
    // PhotonLib cameras are network-table backed; nothing to release explicitly.
  }
}