package frc.robot.autos;

import frc.robot.drivetrain.Drive;
import org.wpilib.math.geometry.Pose2d;
import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.tunable.Selectable;
import org.wpilib.tunable.Tunables;

/**
 * 2027 auto selection. PathPlanner has no 2027 release yet, so the only
 * guaranteed routine is a timed taxi. Add PathPlanner/Choreo routines once a
 * compatible build exists.
 */
public class AutoChooser {
    public interface AutoRoutine {
        void start();

        void run();

        void end();
    }

    private final Selectable<AutoRoutine> chooser = new Selectable<>();
    private final Drive drive;
    private AutoRoutine selected;

    public AutoChooser(Drive drive) {
        this.drive = drive;
        chooser.addDefault("None", new None());
        chooser.add("Taxi (timed)", new TimedTaxi());
        Tunables.publish("Auto choices", chooser);
    }

    public void select() {
        selected = chooser.getSelected();
    }

    public void start() {
        if (selected != null) {
            selected.start();
        }
    }

    public void run() {
        if (selected != null) {
            selected.run();
        }
    }

    public void end() {
        if (selected != null) {
            selected.end();
        }
    }

    public String selectedName() {
        return selected == null ? "None" : selected.getClass().getSimpleName();
    }

    private static final class None implements AutoRoutine {
        public void start() {}

        public void run() {}

        public void end() {}
    }

    private final class TimedTaxi implements AutoRoutine {
        private double startSeconds = -1.0;

        public void start() {
            drive.resetPose(new Pose2d(1.5, 4.0, new Rotation2d()));
            startSeconds = org.wpilib.framework.Timer.getTimestamp();
        }

        public void run() {
            if (startSeconds < 0.0) {
                return;
            }
            if (org.wpilib.framework.Timer.getTimestamp() - startSeconds < 1.8) {
                drive.driveRobotRelative(1.2, 0.0, 0.0);
            } else {
                drive.stop();
            }
        }

        public void end() {
            drive.stop();
            startSeconds = -1.0;
        }
    }
}
