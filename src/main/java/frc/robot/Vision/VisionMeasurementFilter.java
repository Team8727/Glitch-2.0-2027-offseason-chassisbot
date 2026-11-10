package frc.robot.Vision;

import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;

/**
 * Pure vision acceptance and covariance math, kept hardware-free for unit tests.
 */
public final class VisionMeasurementFilter {
    private VisionMeasurementFilter() {}

    public static double linearStdDev(double distanceMeters) {
        return Math.max(
                VisionConstants.MIN_LINEAR_STD_DEV,
                VisionConstants.LINEAR_STD_DEV_BASELINE * distanceMeters * distanceMeters);
    }

    public static boolean isPoseInField(Pose2d pose, AprilTagFieldLayout layout, double marginMeters) {
        double x = pose.getX();
        double y = pose.getY();
        return x >= -marginMeters
                && x <= layout.getFieldLength() + marginMeters
                && y >= -marginMeters
                && y <= layout.getFieldWidth() + marginMeters;
    }

    public static boolean isPoseHeightValid(Pose3d pose) {
        return Math.abs(pose.getZ()) <= VisionConstants.MAX_POSE_Z_METERS;
    }

    public static boolean shouldAcceptSingleTag(double ambiguity, double distanceMeters) {
        return ambiguity >= 0.0
                && ambiguity <= VisionConstants.MAX_AMBIGUITY
                && distanceMeters > 0.0
                && distanceMeters <= VisionConstants.MAX_DISTANCE_METERS;
    }

    public static double normalizeOffsetRotations(double absoluteRotations) {
        double offset = -absoluteRotations;
        offset %= 1.0;
        if (offset >= 0.5) {
            offset -= 1.0;
        } else if (offset < -0.5) {
            offset += 1.0;
        }
        return offset;
    }
}
