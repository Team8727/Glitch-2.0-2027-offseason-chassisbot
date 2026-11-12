package frc.robot;

import edu.wpi.first.math.geometry.Translation2d;

public final class Constants {
    public static final String CAN_BUS = "canivore";

    public static final class CAN {
        // Drive motors (TalonFX)
        public static final int FRONT_LEFT_DRIVE = 1;
        public static final int FRONT_RIGHT_DRIVE = 2;
        public static final int BACK_LEFT_DRIVE = 3;
        public static final int BACK_RIGHT_DRIVE = 4;

        // Steer motors (TalonFX)
        public static final int FRONT_LEFT_STEER = 5;
        public static final int FRONT_RIGHT_STEER = 6;
        public static final int BACK_LEFT_STEER = 7;
        public static final int BACK_RIGHT_STEER = 8;

        // CANcoder absolute encoders
        public static final int FRONT_LEFT_ENCODER = 9;
        public static final int FRONT_RIGHT_ENCODER = 10;
        public static final int BACK_LEFT_ENCODER = 11;
        public static final int BACK_RIGHT_ENCODER = 12;

        // Gyro
        public static final int PIGEON_ID = 13;
    }

    public static final class Swerve {
        public static final double WHEEL_DIAMETER = 0.1016;
        public static final double WHEEL_CIRCUMFERENCE = WHEEL_DIAMETER * Math.PI;

        // WCP MK5n gear ratios
        public static final double DRIVE_RATIO = 6.67;
        public static final double STEER_RATIO = 23.04;

        // Kraken X60 free speed
        public static final double MAX_RPM = 5800;
        public static final double MAX_SPEED_MPS = (MAX_RPM / 60.0) / DRIVE_RATIO * WHEEL_CIRCUMFERENCE;

        // CANcoder offsets (rotations 0-1) — calibrate these with robot facing forward
        public static final double FL_OFFSET = 0.0;
        public static final double FR_OFFSET = 0.0;
        public static final double BL_OFFSET = 0.0;
        public static final double BR_OFFSET = 0.0;

        // Module positions from robot center (meters) — update for your chassis
        public static final double TRACK_WIDTH = 0.635;
        public static final double WHEEL_BASE = 0.635;
        public static final double HALF_TRACK = TRACK_WIDTH / 2.0;
        public static final double HALF_BASE = WHEEL_BASE / 2.0;

        public static final Translation2d FL_LOCATION = new Translation2d(HALF_BASE, HALF_TRACK);
        public static final Translation2d FR_LOCATION = new Translation2d(HALF_BASE, -HALF_TRACK);
        public static final Translation2d BL_LOCATION = new Translation2d(-HALF_BASE, HALF_TRACK);
        public static final Translation2d BR_LOCATION = new Translation2d(-HALF_BASE, -HALF_TRACK);
    }
}
