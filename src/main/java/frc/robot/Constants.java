package frc.robot;

import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;

public final class Constants {

    public static final String CAN_BUS_NAME = "rio";
    public static final int PIGEON_ID = 2;

    // Drive geometry
    public static final double DRIVE_RATIO = 7.01;
    public static final double STEER_RATIO = 26.09;
    public static final double WHEEL_DIAMETER_METERS = 0.1016;
    public static final double WHEEL_CIRCUMFERENCE = WHEEL_DIAMETER_METERS * Math.PI;

    // NEO free speed 5676 RPM
    public static final double MAX_LINEAR_SPEED_MPS =
            5676.0 / DRIVE_RATIO * WHEEL_CIRCUMFERENCE / 60.0;
    public static final double MAX_ANGULAR_SPEED_RADPS = 3.0 * Math.PI;

    // Chassis: 26" x 26" → 0.6604 m track/wheelbase
    public static final double TRACK = 0.6604;
    public static final double WHEELBASE = 0.6604;

    // Module positions from center
    public static final Translation2d FL_POS = new Translation2d(WHEELBASE / 2.0, TRACK / 2.0);
    public static final Translation2d FR_POS = new Translation2d(WHEELBASE / 2.0, -TRACK / 2.0);
    public static final Translation2d BL_POS = new Translation2d(-WHEELBASE / 2.0, TRACK / 2.0);
    public static final Translation2d BR_POS = new Translation2d(-WHEELBASE / 2.0, -TRACK / 2.0);

    public static final SwerveDriveKinematics KINEMATICS =
            new SwerveDriveKinematics(FL_POS, FR_POS, BL_POS, BR_POS);

    // Per-module invert flags
    public static final boolean INVERT_LEFT_SIDE = false;
    public static final boolean INVERT_RIGHT_SIDE = true;

    // --- MODULE CAN IDs (placeholders — REPLACE with actual IDs) ---
    // FL
    public static final int FL_DRIVE_ID = 12;
    public static final int FL_STEER_ID = 11;
    public static final int FL_CANCODER_ID = 13;
    public static final double FL_CANCODER_OFFSET = 0.0;

    // FR
    public static final int FR_DRIVE_ID = 22;
    public static final int FR_STEER_ID = 21;
    public static final int FR_CANCODER_ID = 23;
    public static final double FR_CANCODER_OFFSET = 0.0;

    // BL
    public static final int BL_DRIVE_ID = 32;
    public static final int BL_STEER_ID = 31;
    public static final int BL_CANCODER_ID = 33;
    public static final double BL_CANCODER_OFFSET = 0.0;

    // BR
    public static final int BR_DRIVE_ID = 42;
    public static final int BR_STEER_ID = 41;
    public static final int BR_CANCODER_ID = 43;
    public static final double BR_CANCODER_OFFSET = 0.0;

    // PID placeholders — tune on robot
    public static final double DRIVE_P = 0.0001;
    public static final double DRIVE_I = 0.0;
    public static final double DRIVE_D = 0.0;
    public static final double DRIVE_FF = 0.0001;

    public static final double STEER_P = 0.01;
    public static final double STEER_I = 0.0;
    public static final double STEER_D = 0.0;

    // Joystick
    public static final double JOYSTICK_DEADBAND = 0.1;
    public static final double SPEED_MULTIPLIER = 0.8;
    public static final double ROTATION_MULTIPLIER = 0.6;
}