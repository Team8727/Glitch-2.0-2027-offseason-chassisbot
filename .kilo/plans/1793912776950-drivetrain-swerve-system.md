# Drivetrain System — MK5n Swerve, PhotonVision Localization, Teleop + Auto

## Goal

Deliver a **complete, buildable WPILib 2026 robot project** for the offseason
chassisbot on the **Glitch-bp-manipulation-machine** branch that:

1. Drives an SDS **MK5n** swerve drivetrain (CTRE Phoenix 6).
2. Self-localizes via swerve odometry fused with **two PhotonVision AprilTag
   cameras** (front-left + front-right on a Rubik Pi coprocessor).
3. Is drivable remotely (Xbox field-centric teleop) and in **PathPlanner auto**.

The branch currently contains only the CANdle LED subsystem sources and no build
scaffolding, so the full project (Gradle scaffold, vendordeps, entry points) is
created from scratch. **User override of AGENTS.md:** work on branch
`Glitch-bp-manipulation-machine`; the old `Milo` branch is ignored.

## Decisions (confirmed with user)

| Topic | Decision |
|---|---|
| Branch | `Glitch-bp-manipulation-machine`, all work committed there (never `main`). Full scaffold created fresh; ignore `origin/Milo` |
| Swerve layer | CTRE **Phoenix 6 `SwerveDrivetrain`** — `TunerConstants` + `CTRESwerveDrivetrain` (2026 convention) |
| Motors | **Kraken X60** drive + **Kraken X44** steer (TalonFX, integrated), CTRE **CANcoder** azimuth, `TalonFX_Integrated` arrangements |
| Gyro | CTRE **Pigeon 2**, CAN ID **2**, roboRIO construction CAN bus |
| CAN bus | roboRIO bus (all devices incl. CANdle ID 14) |
| IDs / geometry | 2026 defaults as tunable constants: FL 11/12/13, FR 21/22/23, BL 31/32/33, BR 41/42/43 (steer/drive/encoder); Pigeon 2 ID 2; wheel radius 2"; X/Y offsets ±10.375" (20.75" x 20.75") |
| Gear ratios | Steer **287:11 ≈ 26.0909** (fixed MK5n spec). Drive = **standard MK5n ratio (~6.12:1)** as default, **flagged VERIFY** (candidates in constants; 2026 robot used 7.03125). Coupling ratio default 4.5, VERIFY |
| GlitchLib | **Dropped.** Fire patterns ported locally (`LocalLEDPatterns.java`, adapted from the team's `james` branch port) |
| Vision | PhotonLib v2026.2.2; two cameras `FrontLeft`/`FrontRight` on Rubik Pi running PhotonVision; camera names + transforms as tunable constants (2026 defaults: yaw ±10°, z=28", x=2.375", y=+3.25/-2.875 in) |
| Field layout | `AprilTagFieldLayout.loadField(AprilTagFields.k2026RebuiltAndymark)` default; swap constant when 2027 layout lands |
| Teleop | `CommandXboxController` (port 0), field-centric `SwerveRequest.FieldCentric`, deadbands, stopped-brake, heading reset (2026 convention) |
| Auto | PathPlanner 2026.1.2 via `AutoBuilder` + `Autos` chooser with a bundled test path |
| LEDs | Existing `CandleLEDs`/`LEDSubsystem` reused; only `GlitchLib` import replaced with `LocalLEDPatterns`; mode hooks wired in `Robot.java` |

## Ordered task list

### 1. Project scaffold (create fresh)

- `settings.gradle`, `build.gradle` (GradleRIO **2026.2.1**, Java 17,
  `ROBOT_MAIN_CLASS = "frc.robot.Main"`, desktop sim, JUnit 5 — copy the 2026
  repo structure **minus the GlitchLib source-set entries**).
- Gradle wrapper: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`
  (8.11), `gradle/wrapper/gradle-wrapper.properties`.
- `.gitignore` (WPILib/Gradle/IDE artifacts), `.wpilib/wpilib_preferences.json`
  (team 8727, projectYear 2026).
- `src/main/java/frc/robot/Main.java` (standard WPILib launcher).
- **No** `.gitmodules` / GlitchLib submodule.
- Vendordeps: `Phoenix6-26.1.1.json`, `photonlib.json` (v2026.2.2),
  `PathplannerLib-2026.1.2.json`, `WPILibNewCommands.json`. Skip REV/Studica/URCL
  unless needed for other subsystems.

### 2. Port LED patterns (drop GlitchLib)

- `src/main/java/frc/robot/Subsystems/LocalLEDPatterns.java`: port
  `Glitch.lib.LEDs.GlitchLEDPatterns.fire(...)` locally (the `james` branch
  already did this — reuse its implementation in
  `frc.robot.Subsystems`, keep signature `fire(LEDPattern, double updateTime,
  Color shiftColor, double speed)`).
- Edit `LEDSubsystem.java`: replace
  `import Glitch.Lib.LEDs.GlitchLEDPatterns;` with the local import; keep the
  existing subsystem API, constants, CANdle ID 14, and lifecycle methods
  unchanged. `CandleLEDs.java` needs no change (no GlitchLib references).

### 3. Drivetrain — `frc.robot.Drivetrain`

- `TunerConstants.java`: full MK5n CTRE swerve config (2026 convention layout):
  - Steer gains (kP≈100, kD≈0.5, kS 0.1, kV 2.49), drive gains (kP 0.1, kV
    0.124), Voltage closed-loop output, `FusedCANcoder` feedback, slip current
    ~80 A, stator/supply current limits (drive 50/80 A, steer 40/30 A).
  - `DriveMotorArrangement.TalonFX_Integrated` +
    `SteerMotorArrangement.TalonFX_Integrated`.
  - `CANBus` = roboRIO bus (`new CANBus()`), Pigeon 2 ID 2.
  - **VERIFY block**: `kDriveGearRatio = 6.12` (MK5n standard; candidates
    5.36 fast / 6.12 std / 7.36 slow, and 2026 robot used 7.03125 — confirm the
    installed MK5n pinion), `kCoupleRatio = 4.5`, `kWheelRadius = 2"`,
    `kSpeedAt12Volts ≈ 4.39 m/s` placeholder.
  - Modules: defaults FL 11/12/13, FR 21/22/23, BL 31/32/33, BR 41/42/43
    (steer/drive/CANcoder), X/Y ±10.375", encoder offsets = 0.0 placeholders +
    `// VERIFY: set from Tuner X` note.
  - Config listeners, diff encoders wiring, max speeds; `createDrivetrain()`
    factory returning `CTRESwerveDrivetrain`.
- `CTRESwerveDrivetrain.java` (`extends TunerConstants.TunerSwerveDrivetrain
  implements Subsystem`): 2026-convention wrapper —
  - `applyRequest(Supplier<SwerveRequest>)`, sim thread (4 ms Notifier) when
    simulated, operator-perspective (blue 0° / red 180°) applied each cycle,
    SysId routines for translation/steer/rotation (optional but keep from 2026).
  - `periodic()`: calls `vision.periodic()`, drains `Vision.Measurement` list
    using `getState().Pose` as reference, calls `addVisionMeasurement(...)` with
    `Utils.fpgaToCurrentTime(...)`; counts measurements.
  - PathPlanner `AutoBuilder.configure(...)` with holonomic controller
    (linear kP 10, angular kP 7), `RobotConfig.fromGUISettings()`, and alliance
    function.
  - `setVision(Vision)`, seed/reset helpers, `getRobotPose()`.
- `Telemetry.java`: reuse the 2026 Telemetry (DriveState NT topics, Module
  Mechanism2d widgets, Field2d pose) with `MaxSpeed` injected.

### 4. Vision — `frc.robot.Vision`

Direct PhotonLib facade (no GlitchLib):
- Constants: camera names `"FrontLeft"` / `"FrontRight"`, their
  `Transform3d`s (2026 defaults shown above, clearly marked tunable), field
  layout (2026 RebuiltAndymark), `MAX_AMBIGUITY = 0.2`,
  `MAX_DISTANCE_METERS = 3.5`, per-camera stddev gating, sim props (640x480,
  FOV 70°, 40 FPS, ~35 ms latency).
- Constructor: one `PhotonCamera` + one `PhotonPoseEstimator` (strategy
  `MULTI_TAG_PNP_ON_COPROCESSOR`, fallback documented) per camera.
- `periodic()` refreshes latest results.
- `drainMeasurements(Pose2d reference)`: for each camera convert each tagged
  pose to a `Measurement(pose, timestampSeconds)` applying ambiguity/distance
  filters (reject empty/ambiguity > 0.2/distance > 3.5 m; use reference pose to
  disambiguate multi-camera overlap when needed).
- `bestStartPose()` → initial pose from visible tags (optional, used at boot).
- `logCameraPoses(Pose2d)` → NT Field2d/pose3d for Glass/AdvantageScope.
- Sim: single `PhotonSimVisionSystem`/sim cameras when `Robot.isSimulation()`.

### 5. Controls — `frc.robot.controller`

- `CTReSwerveControls.java` (2026 style): sets drivetrain default command =
  `FieldCentric` with `MaxSpeed = 80%` of theoretical speed,
  `MaxAngularRate = 0.75 rot/s`, 10% deadbands, open-loop voltage drive.
  - Left stick Y → forward, left stick X → strafe, right stick X → rotate.
  - Sticks-at-rest → automatic `SwerveDriveBrake`; disabled → `Idle`.
  - Start button → `seedFieldCentric` heading reset.

### 6. Auto — `frc.robot.Autos`

- 2026-convention `Autos` class: register nothing extra (drive-only), load
  paths from `deploy/pathplanner/`, publish `SendableChooser<Command>` with
  mirrored variants.
- Bundle one simple `test`/`drive test` path JSON under
  `src/main/deploy/pathplanner/paths/` so the chooser has a default that works
  out of the box (plus `settings.json`/`navgrid.json` if needed).
- `followPathFromStartPose(path, mirror)` → set start pose (flipped for red)
  then `AutoBuilder.followPath`.

### 7. `Robot.java`

- Construct: `CTRESwerveDrivetrain` via `TunerConstants.createDrivetrain()`,
  `Vision`, `CTReSwerveControls`, `Autos`, `LEDSubsystem.getInstance()`.
- `drivetrain.setVision(vision)`; PathPlanner warmup; DataLogManager + NT logs.
- Mode hooks: `disabledInit` → LED `start()`; `autonomousInit` →
  `cancelAll()` + schedule selected auto + `leds.autoInit()`; `teleopInit` →
  `cancelAll()` + `leds.teleopInit()`; `testInit` → `leds.testInit()`.
- `isRedAlliance()` helper (from DriverStation).

## Validation

- `./gradlew build` (compile, fat jar) and `./gradlew :test` (JUnit present;
  optionally a Telemetry/constants sanity test).
- Sim (`./gradlew simulateJava`): drive with DS, camera sim shows poses, auto
  chooser loads, pose estimator fuses vision.
- Bench/hardware checklist:
  - Tuner X: all 9 CAN devices enumerate (4 steer, 4 drive, 4 CANcoders,
    Pigeon 2 ID 2, CANdle 14).
  - Set each CANcoder **absolute offset** in TunerConstants via Tuner X
    (wheels forward = 0°).
  - Confirm installed MK5n drive pinion ratio and set `kDriveGearRatio`.
  - Spin-up: wheels rotate/steer without brownout; field-centric drive correct
    with Pigeon 2 orientation.
  - PhotonVision UI on the Rubik Pi: both cams stream, apriltags detected,
    transforms match code; drive in circles and verify Field2d pose tracks.

## Risks / notes

- **Gear ratio & coupling ratio** and **CANcoder offsets** cannot be known from
  code — they are clearly marked `VERIFY` constants (offsets default 0.0).
- **Phoenix 6 26.1.1 API drift**: implementer must compile against the vendored
  version and adjust renamed config fields rather than guessing.
- **Vision fusion widgets**: start with conservative stddevs; increase trust once
  measured transforms are confirmed.
- **Branch rule**: user explicitly overrode the AGENTS.md `Milo` rule — do all
  commit work on `Glitch-bp-manipulation-machine` and never on `main`.
- If the 2027 field layout is published before implementation, switch the
  `FIELD_LAYOUT` constant accordingly.

## Out of scope

- Game-piece mechanisms (intake/shooter/etc.), zone controllers, projection math.
- 2027 game field layout details, PathPlanner path design (consumer-side GUI work).
- GlitchLib maintenance or submodule re-add.