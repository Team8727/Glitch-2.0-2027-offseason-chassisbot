package frc.robot.telemetry;

import org.wpilib.networktables.BooleanPublisher;
import org.wpilib.networktables.DoublePublisher;
import org.wpilib.networktables.NetworkTable;
import org.wpilib.networktables.NetworkTableInstance;
import org.wpilib.networktables.StringPublisher;

/**
 * Explicit NT4 publishers for Elastic. Same topic design as Beako-RIO.
 * 2027 removes SmartDashboard, so this class is the whole dashboard surface.
 */
public class Telemetry {
    private final NetworkTable elastic =
            NetworkTableInstance.getDefault().getTable("Elastic");

    private final DoublePublisher poseX = elastic.getDoubleTopic("Drive/PoseX").publish();
    private final DoublePublisher poseY = elastic.getDoubleTopic("Drive/PoseY").publish();
    private final DoublePublisher headingDeg = elastic.getDoubleTopic("Drive/HeadingDeg").publish();
    private final DoublePublisher batteryVolts = elastic.getDoubleTopic("Robot/BatteryVolts").publish();
    private final DoublePublisher loopTimeMs = elastic.getDoubleTopic("Robot/LoopTimeMs").publish();
    private final BooleanPublisher visionActive = elastic.getBooleanTopic("Vision/Active").publish();
    private final DoublePublisher visionAccepted =
            elastic.getDoubleTopic("Vision/AcceptedMeasurements").publish();
    private final StringPublisher ledState = elastic.getStringTopic("LED/State").publish();

    public void publishDrive(org.wpilib.math.geometry.Pose2d pose) {
        poseX.set(pose.getX());
        poseY.set(pose.getY());
        headingDeg.set(pose.getRotation().getDegrees());
    }

    public void publishHealth(double battery, double loopMs, boolean vision, int accepted, String leds) {
        batteryVolts.set(battery);
        loopTimeMs.set(loopMs);
        visionActive.set(vision);
        visionAccepted.set(accepted);
        ledState.set(leds);
    }

    public void publishBattery(double volts) {
        batteryVolts.set(volts);
    }
}
