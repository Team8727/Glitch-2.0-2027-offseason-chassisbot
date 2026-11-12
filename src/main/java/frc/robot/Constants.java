package frc.robot;

import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.util.Units;

public final class Constants {

    // ── CAN Bus ───────────────────────────────────────────────────────────
    public static final String CANIVORE_BUS = "CANivore";
    public static final String RIO_CAN_BUS = "rio";

    // ── CAN IDs (CANivore bus) ────────────────────────────────────────────
    // MK5n Swerve Modules (NEO drive + NEO 550 steer)
    // Drive: NEO on SPARK MAX via CANivore
    // Steer: NEO 550 on SPARK MAX via CANivore
    // CANcoder on CANivore
    public static final int PIGEON_ID = 30;

    // FL
    public static final int FL_DRIVE_ID = 1;
    public static final int FL_STEER_ID = 2;
    public static final int FL_CANCODER_ID = 3;
    public static final double FL_CANCODER_OFFSET = 0.0; // tune on robot

    // FR
    public static final int FR_DRIVE_ID = 4;
    public static final int FR_STEER_ID = 5;
    public static final int FR_CANCODER_ID = 6;
    public static final double FR_CANCODER_OFFSET = 0.0;

    // BL
    public static final int BL_DRIVE_ID = 7;
    public static final int BL_STEER_ID = 8;
    public static final int BL_CANCODER_ID = 9;
    public static final double BL_CANCODER_OFFSET = 0.0;

    // BR
    public static final int BR_DRIVE_ID = 10;
    public static final int BR_STEER_ID = 11;
    public static final int BR_CANCODER_ID = 12;
    public static final double BR_CANCODER_OFFSET = 0.0;

    // CANdle
    public static final int CANDLE_ID = 21;

    // ── Drive Geometry (MK5n) ────────────────────────────────────────────
    public static final double DRIVE_RATIO = 7.01;
    public static final double STEER_RATIO = 26.09;
    public static final double WHEEL_DIAMETER_METERS = Units.inchesToMeters(4.0);
    public static final double WHEEL_CIRCUMFERENCE = WHEEL_DIAMETER_METERS * Math.PI;

    // NEO free speed 5676 RPM
    public static final double MAX_LINEAR_SPEED_MPS =
            5676.0 / DRIVE_RATIO * WHEEL_CIRCUMFERENCE / 60.0;
    public static final double MAX_ANGULAR_SPEED_RADPS = 3.0 * Math.PI;

    // Chassis: 26" x 26" track/wheelbase
    public static final double TRACK = Units.inchesToMeters(26.0);
    public static final double WHEELBASE = Units.inchesToMeters(26.0);

    public static final Translation2d FL_POS = new Translation2d(WHEELBASE / 2.0, TRACK / 2.0);
    public static final Translation2d FR_POS = new Translation2d(WHEELBASE / 2.0, -TRACK / 2.0);
    public static final Translation2d BL_POS = new Translation2d(-WHEELBASE / 2.0, TRACK / 2.0);
    public static final Translation2d BR_POS = new Translation2d(-WHEELBASE / 2.0, -TRACK / 2.0);

    public static final SwerveDriveKinematics KINEMATICS =
            new SwerveDriveKinematics(FL_POS, FR_POS, BL_POS, BR_POS);

    // ── Invert Flags ──────────────────────────────────────────────────────
    public static final boolean INVERT_LEFT_SIDE = false;
    public static final boolean INVERT_RIGHT_SIDE = true;

    // ── Drive PID / FF (tune on robot) ────────────────────────────────────
    public static final double DRIVE_P = 0.0001;
    public static final double DRIVE_I = 0.0;
    public static final double DRIVE_D = 0.0;
    public static final double DRIVE_FF = 0.00015;
    public static final double DRIVE_KS = 0.0;
    public static final double DRIVE_KV = 0.0;
    public static final double DRIVE_KA = 0.0;

    public static final double STEER_P = 0.01;
    public static final double STEER_I = 0.0;
    public static final double STEER_D = 0.0;

    // ── Joystick ──────────────────────────────────────────────────────────
    public static final int DRIVER_CONTROLLER_PORT = 0;
    public static final double JOYSTICK_DEADBAND = 0.08;
    public static final double SPEED_MULTIPLIER = 0.8;
    public static final double ROTATION_MULTIPLIER = 0.65;
    public static final double HEADING_LOCK_DEADBAND = 0.08;

    // ── LEDs ──────────────────────────────────────────────────────────────
    public static final int LED_COUNT = 60;
    public static final double LED_BRIGHTNESS = 0.8;
    public static final double FIRE_SPARKING = 1.0;
    public static final double FIRE_COOLING = 0.73;
    public static final double FIRE_FRAME_RATE = 60.0;

    // ── Vision ────────────────────────────────────────────────────────────
    public static final String CAMERA_FRONT_LEFT_NAME = "FrontLeft";
    public static final String CAMERA_FRONT_RIGHT_NAME = "FrontRight";
    public static final String PHOTONVISION_HOST = "photonvision.local";

    // ── Autos ─────────────────────────────────────────────────────────────
    public static final double AUTO_MAX_SPEED_MPS = MAX_LINEAR_SPEED_MPS * 0.6;
    public static final double AUTO_MAX_ACCEL_MPS2 = 2.0;
    public static final double AUTO_MAX_ANGULAR_SPEED_RADPS = Math.PI;

    // ── Feature Flags ─────────────────────────────────────────────────────
    public static final boolean ENABLE_VISION = true;
    public static final boolean ENABLE_LEDS = true;
    public static final boolean ENABLE_HEADING_LOCK = true;
    public static final boolean TUNING_MODE = false;
}