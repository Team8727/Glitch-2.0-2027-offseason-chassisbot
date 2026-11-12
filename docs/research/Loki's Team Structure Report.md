# Loki's Team Structure Report

**Prepared:** November 10, 2026  
**Scope:** Four public 2026 FRC REBUILT robot codebases, compared with Glitch 2.0's 2026 chassisbot.  
**Method:** Each repository was cloned at the SHA below and read directly. Findings are cited by path and line where the repository supports it. No team code was copied.

## 1. Executive summary

The four teams solve the same problem in four different ways:

| Team | Repo | SHA | Dominant idea |
|---|---|---|---|
| 1678 Citrus Circuits | `frc1678/C2026-Public` | `0f403dd` | Library-first structure, IO seams for mechanisms, Choreo autos |
| 9496 Lynk | `LynkRobotics/RobotCode2026Public` | `6f60fc9` | Practical subsystem singletons, DogLog, native CANdle fire animations |
| 6328 Mechanical Advantage | `Mechanical-Advantage/RobotCode2026Public` | `5f53c92` | AdvantageKit logging/replay, timestamped `RobotState`, IO interfaces |
| 5940 BREAD | `BREAD5940/2026-Onseason` | `e06f4ff` | Uniform `updateInputs -> processState`, AdvantageKit, strong vision noise model |

The consistent winners are **a hardware seam (IO), a single authoritative pose/state object, replayable logging, and a boring, small auto pipeline**. The consistent tax is **bespoke infrastructure**: 6328's split-process robot, 1678's shooting/regression stack, 9496's two drive controllers, 5940's generated logging layer. Glitch should copy the first group and refuse the second.

## 2. What each team does well

### 1678 Citrus Circuits

- **Reusable library layer.** `frc.lib` separates bases, IO, logging, math, simulation, and utilities from season code. Subsystems follow `X.java` + `XConstants.java`.
- **Mechanism IO seam.** `MotorIO` + `Setpoint` + `MotorSubsystem` + `MotorIOTalonFXSim` gives real and simulated mechanisms one contract (`src/main/java/frc/lib/io/MotorIO.java:23`, `MotorSubsystem.java:145`).
- **Loop instrumentation.** `LoggedTracer`, cycle-time logging, and a try/catch that publishes the last loop error keep performance and crashes visible (`src/main/java/frc/robot/Robot.java:152`).
- **Choreo autos with mirroring.** One left/right routine family instead of duplicated autos, plus explicit starting-pose reset (`autos/AutoModeSelector.java:29`, `Robot.java:212`).
- **Formatting gate.** Spotless + palantirJavaFormat + cleanthat (`build.gradle:142`).

**Avoid:** static singletons and static-init side effects (`Drive.mInstance` at `Drive.java:45`, `RobotConstants.java:35`); per-robot ternaries in shared constants (`TunerConstants.java:31`); the 1000-line `Superstructure` and five-package shooting/regression stack; tests disabled from `build` (`build.gradle:114`).

### 9496 Lynk

- **Named command logging.** `LoggedCommands` mirrors the Commands API and logs start/finish, so every auto step has a name in the log (`src/main/java/frc/lib/util/LoggedCommands.java:19`).
- **Layered vision rejection.** Multi-tag preferred, ambiguity ceiling for single tags, alternate-pose selection by gyro heading, distance²/tag-count standard deviations, field/Z bounds, and tilt-gated odometry (`subsystems/vision/Vision.java:93`, `Pose.java:245`).
- **Native CANdle animations.** `CANdleGroup` and `CustomFireAnimation` render on-device instead of streaming pixels from the rio (`src/main/java/frc/lib/util/CANdleGroup.java`, `CustomFireAnimation.java`).
- **Distance-indexed calibration table** with a unit test (`CalibrationTable`, `CalibrationTableTest.java:28`).
- **Git-describe version stamping** and per-motor current/temp/stall logging (`build.gradle` `writeGitInfo`, `LynkSubsystem.java:58`).

**Avoid:** singleton construction order (explicit ordered array at `Robot.java:87`); no IO layer and no desktop simulation (`build.gradle` `includeDesktopSupport = false`); `Timer.delay(0.1)` CANcoder workaround (`SwerveModule.java:107`); two parallel drive controllers (PathPlanner + bespoke `PIDSwerve`) and hand-coded 400-line auto trees (`autos/Autos.java:363`).

### 6328 Mechanical Advantage

- **The reference logging architecture.** AdvantageKit `@AutoLog`, `Logger.processInputs`, `@AutoLogOutput`, real/sim/replay IO implementations, and a replayable `.wpilog` pipeline (`Robot.java:108`, `subsystems/drive/ModuleIO.java:18`).
- **One timestamped `RobotState`.** A 2-second pose buffer and closed-form Kalman fusion with latency-compensated observations (`RobotState.java:33`, `186`).
- **Clean IO boundary.** `ModuleIO` input/output structs; `ModuleIOSim` uses `DCMotorSim`; replay uses no-op IO (`RobotContainer.java:252`).
- **Build-enforced hygiene.** Spotless runs before compile; constants checks block PRs with non-default robot settings (`build.gradle:319`, `378`).
- **Per-stage timing.** `LoggedTracer` records loop stages (`util/LoggedTracer.java:16`).

**Avoid:** the Mac mini + roboRIO split process and custom protobuf (Idun), which 6328 itself says SystemCore removes (`idun/README.md:8`); four-camera Northstar Python/Metal detection; 200 Hz SYNC loop and custom compression; ~2,000 lines of custom auto builders and VTS codegen.

### 5940 BREAD

- **Uniform periodic contract.** Every subsystem implements `updateInputs()` then `processState()`, so the robot loop is predictable (`Robot.java:320`).
- **Strong vision noise model.** Power-law anisotropic standard deviations rotated by tag-ray angle, plus ambiguity/distance/border rejection (`Vision.java:46`, `505`).
- **Practical IO split.** Abstract `*IO` with Real/Sim/Disabled implementations; Maple-Sim for swerve (`ShooterIO.java:14`, `Swerve.java:143`).
- **Zero-boilerplate tuning.** `@tune` fields are discovered, bound to NT, and written back reflectively (`TuneManager.java:66`).
- **CANdle readiness checklist** in disabled, so the robot tells the driver when it is safe (`Robot.java:350`).

**Avoid:** 617-line `LoggedTalonFX` with background config threads (`LoggedTalonFX.java:51`); 23 duplicated `B*` control wrappers; javac-plugin `--add-exports` build hacks (`build.gradle:124`); 28 static `PathPlannerPath` fields + reflection-based auto pose lookup (`AutonomousSelector.java:85`); no tests and a pre-commit script that runs `-x test`.

## 3. What Glitch 2026 does well that they do not

| Glitch strength | Evidence | Why it matters |
|---|---|---|
| Coordinate-frame and unit comments are unusually explicit | `src/main/java/frc/robot/Drivetrain/CTRESwerveDrivetrain.java`, `Vision/Vision.java` | Prevents the exact class of field-frame bugs that cost matches |
| Vision covariance scales with distance and tag count | `Vision/Vision.java` | More honest than a fixed standard deviation |
| Multiple logging sinks already wired (DogLog, DataLog, URCL, SignalLogger) | `Robot.java`, `Drivetrain/DrivetrainTelemetry.java` | The raw material for a replay pipeline is already there |
| Working CANdle fire patterns and Elastic notifications | `Subsystems/StatusLEDs.java` (2027), 2026 LED code | Driver feedback is already a design habit |
| SysId routines and a real PathPlanner setup | `Robot.java`, `Autos/Autos.java` | Ahead of teams that only drive open-loop |
| Small CI + unit tests exist | `.github/workflows/ci.yml`, `src/test` | Most of the four teams do not enforce either |
| A written context/known-issues system | Agent Context repo | None of the four teams has an equivalent AI-facing source of truth |

The honest caveat: Glitch has the **ingredients** of good practice but not the **enforcement**. The 2026 code mixes five logging stacks, keeps a monolithic `Robot`, and does not run a command-based architecture. The four teams above enforce one path; Glitch currently permits several.

## 4. Cross-team comparison

| Area | 1678 | 9496 | 6328 | 5940 | Glitch 2026 |
|---|---|---|---|---|---|
| Architecture | `frc.lib` + season | `frc.lib` + singletons | IO + `RobotState` | Container + state machines | TimedRobot monolith |
| Drivetrain | CTRE, no IO | CTRE, no IO | Custom + IO | CTRE, no IO | CTRE, no IO |
| Logging | DataLog + NT | DogLog | AdvantageKit | AdvantageKit + custom | 5 stacks |
| Replay | No | No | Yes | Partial | No |
| Vision | Photon/Limelight + CTRE | Photon + layered rejection | Northstar + custom fusion | Photon + anisotropic noise | Photon + distance scaling |
| Autos | Choreo + mirror | PathPlanner + custom | Choreo + generated | PathPlanner + custom | PathPlanner |
| Tests | 1 live | 1 | 1 smoke | 0 | 2 |
| CI | Yes (stale image) | No | No (Gradle checks) | No | Yes |
| Formatting | Spotless | None | Spotless | None | Spotless |
| Dashboard | Elastic (manual) | Elastic (5 tabs) | Elastic (2 tabs) | None | Elastic/SmartDashboard |
| Tuning | TunableNumber | NT toggles | LoggedTunableNumber | `@tune` | Dashboard + constants |

## 5. Practices Glitch should adopt, in priority order

### P0 — before the 2027 kickoff

1. **One authoritative robot state.** Create a `RobotState` that owns pose, velocities, and vision measurements; make drivetrain/vision write into it. Copy 6328's timestamp discipline, not its split process (`6328 RobotState.java:186`).
2. **One logging path.** DogLog for humans, WPILib DataLog for replay, CTRE SignalLogger only for raw motor data. Delete the duplicate NT logger. Add `LoggedTracer`-style per-stage timing.
3. **An IO seam for the drivetrain and cameras.** `DriveIO`/`VisionIO` with Real/Sim implementations. This is the single change that makes autos testable on a laptop.
4. **Named commands everywhere.** Adopt 9496's `LoggedCommands` idea so every auto step is searchable in the log (`9496 LoggedCommands.java:19`).
5. **A real test/CI gate.** `./gradlew build` must run tests, Spotless must run before compile, and CI must fail on either.

### P1 — first build week

6. **Vision rejection as a policy object.** Distance² scaling, ambiguity ceiling, multi-tag preference, and a stale/tilt odometry guard, all in one testable class.
7. **Auto selection that cannot silently run the wrong path.** Validate the starting pose against the selected path; log the chosen auto name and starting pose.
8. **A single CANdle abstraction.** Native animations on-device, state changes only, health logging (see `9496 CANdleGroup`).
9. **Constants with one robot identity.** One `RobotConfig` constant, no env-var ternaries sprinkled through shared constants.

### P2 — later

10. **AdvantageKit replay** once the IO seam exists. Do not start with it; start with the seam.
11. **Choreo or PathPlanner GUI autos.** Keep paths out of Java.
12. **A build-time constants check** that fails if competition/tuning flags are wrong (6328 `checkConstantsDeploy`).

## 6. What not to copy

- **6328's split-process robot.** It is brilliant and wrong for a drive-only offseason chassis without a Mac mini and a C++ build chain.
- **1678's shooting/regression/verification tree.** The chassis has no shooter.
- **9496's dual drive controllers and hand-built autos.** One path pipeline is enough.
- **5940's generated logging/annotation layer.** The complexity is not repaid at Glitch's size.
- **Any static singleton construction order.** Use constructor injection and explicit lifecycle.
- **Any code without a license.** The clones are for study; patterns only.

## 7. Concrete changes for the 2027 chassis

| Change | File/class |
|---|---|
| Add `DriveIO`/`VisionIO` interfaces + sim implementations | `frc/robot/drivetrain`, `frc/robot/vision` |
| Add `RobotState` with pose buffer and timestamps | `frc/robot/RobotState.java` |
| Replace scattered logging with `Telemetry` + `LoggedTracer` | `frc/robot/telemetry` |
| Add `LoggedCommand` wrapper | `frc/robot/util/LoggedCommand.java` |
| Add `VisionPolicy` with ambiguity/distance/tilt rules | `frc/robot/vision/VisionPolicy.java` |
| Add `AutoChoice` with start-pose validation | `frc/robot/autos/AutoChooser.java` |
| Add CANdle state machine + native animations | `frc/robot/leds/CandleLEDs.java` |
| Add build checks for tests + formatting | `build.gradle`, `.github/workflows/ci.yml` |

## 8. Bottom line

Glitch does not need to become 6328 or 1678. It needs to make three things non-negotiable: **one pose/state object, one logging path, and one tested auto pipeline**. Those three changes buy most of the reliability the top teams have, without importing their hardware-specific complexity.
