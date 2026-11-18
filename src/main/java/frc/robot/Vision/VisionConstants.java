package frc.robot.Vision;

import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.apriltag.AprilTagFields;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;

/**
 * Robot-specific vision configuration.
 *
 * <p>Camera transforms are TODO until the RubikPi mounts are measured on the
 * 2027 robot. Real-robot fusion is intentionally disabled until
 * {@link #CAMERA_TRANSFORMS_CONFIGURED} is flipped to true.
 */
public final class VisionConstants {
    private VisionConstants() {}

    public static final String FRONT_LEFT_CAMERA = "FrontLeft";
    public static final String FRONT_RIGHT_CAMERA = "FrontRight";

    /**
     * TODO: measure the front-left RubikPi camera mount relative to robot center.
     * Placeholder uses a 0.30 m forward, 0.17 m left, 0.71 m up, 10 deg outward mount.
     */
    public static final Transform3d ROBOT_TO_FRONT_LEFT = new Transform3d(
            new Translation3d(0.30, 0.17, 0.71),
            new Rotation3d(0.0, 0.0, Math.toRadians(10.0)));

    /**
     * TODO: measure the front-right RubikPi camera mount relative to robot center.
     * Placeholder mirrors the left camera to 0.17 m right.
     */
    public static final Transform3d ROBOT_TO_FRONT_RIGHT = new Transform3d(
            new Translation3d(0.30, -0.17, 0.71),
            new Rotation3d(0.0, 0.0, Math.toRadians(-10.0)));

    /** Flip to true only after both transforms above are measured. */
    public static final boolean CAMERA_TRANSFORMS_CONFIGURED = false;

    public static final AprilTagFieldLayout FIELD_LAYOUT =
            AprilTagFieldLayout.loadField(AprilTagFields.k2026RebuiltAndymark);

    public static final double MAX_AMBIGUITY = 0.2;
    public static final double MAX_DISTANCE_METERS = 4.0;
    public static final double MAX_MEASUREMENT_AGE_SECONDS = 0.5;
    public static final double LINEAR_STD_DEV_BASELINE = 0.10;
    public static final double MIN_LINEAR_STD_DEV = 0.05;
    public static final double FIELD_MARGIN_METERS = 1.0;
    public static final double MAX_POSE_Z_METERS = 0.5;

    public static final int SIM_WIDTH = 1280;
    public static final int SIM_HEIGHT = 720;
    public static final double SIM_FOV_DEG = 70.0;
    public static final int SIM_FPS = 30;
    public static final double SIM_AVG_LATENCY_MS = 35.0;
    public static final double SIM_LATENCY_STD_DEV_MS = 5.0;
}
