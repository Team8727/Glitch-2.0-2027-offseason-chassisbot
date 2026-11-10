package frc.robot.Vision;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisionMeasurementFilterTest {
    @Test
    void acceptsPoseInsideField() {
        assertTrue(VisionMeasurementFilter.isPoseInField(
                new Pose2d(3.0, 4.0, Rotation2d.kZero),
                VisionConstants.FIELD_LAYOUT,
                VisionConstants.FIELD_MARGIN_METERS));
    }

    @Test
    void rejectsPoseOutsideMargin() {
        assertFalse(VisionMeasurementFilter.isPoseInField(
                new Pose2d(-2.0, 4.0, Rotation2d.kZero),
                VisionConstants.FIELD_LAYOUT,
                VisionConstants.FIELD_MARGIN_METERS));
    }

    @Test
    void rejectsBadPoseHeight() {
        assertTrue(VisionMeasurementFilter.isPoseHeightValid(
                new Pose3d(0.0, 0.0, 0.1, new edu.wpi.first.math.geometry.Rotation3d())));
        assertFalse(VisionMeasurementFilter.isPoseHeightValid(
                new Pose3d(0.0, 0.0, 1.0, new edu.wpi.first.math.geometry.Rotation3d())));
    }

    @Test
    void scalesLinearStdDevWithDistance() {
        assertEquals(0.05, VisionMeasurementFilter.linearStdDev(0.0), 1e-9);
        assertEquals(0.4, VisionMeasurementFilter.linearStdDev(2.0), 1e-9);
        assertTrue(VisionMeasurementFilter.linearStdDev(2.0)
                > VisionMeasurementFilter.linearStdDev(1.0));
    }

    @Test
    void acceptsOnlyLowAmbiguityCloseTags() {
        assertTrue(VisionMeasurementFilter.shouldAcceptSingleTag(0.1, 2.0));
        assertFalse(VisionMeasurementFilter.shouldAcceptSingleTag(0.3, 2.0));
        assertFalse(VisionMeasurementFilter.shouldAcceptSingleTag(-1.0, 2.0));
        assertFalse(VisionMeasurementFilter.shouldAcceptSingleTag(0.1, 5.0));
    }

    @Test
    void normalizesSteerOffsets() {
        assertEquals(-0.1, VisionMeasurementFilter.normalizeOffsetRotations(0.1), 1e-9);
        assertEquals(0.1, VisionMeasurementFilter.normalizeOffsetRotations(0.9), 1e-9);
        assertEquals(0.1, VisionMeasurementFilter.normalizeOffsetRotations(-0.1), 1e-9);
    }
}
