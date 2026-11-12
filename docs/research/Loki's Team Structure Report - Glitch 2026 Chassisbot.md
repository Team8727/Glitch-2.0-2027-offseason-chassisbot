# Loki's Team Structure Report — Glitch 2026 Chassisbot

**Prepared:** November 10, 2026  
**Target:** `Glitch-2.0-2026-chassisbot` (2026 REBUILT competition robot)  
**Method:** Direct code read. Claims below are checked against the repository, not the README.  
**Context:** THOR West 2026 finished rank 17 of 20 (1-5-0), Alliance 6, eliminated Round 3. AUTO fuel averaged 16.33; TOWER averaged 0.00 because the codebase has no climber.

## 1. Short version

The 2026 robot is a **strong hardware-integration codebase with a weak software architecture**. The team clearly understands swerve, vision, coordinate frames, and logging at a level most teams do not. What it lacks is the discipline that turns that knowledge into repeatable match performance: one source of truth, one logging path, one command structure, and tests that actually gate a merge.

Keep the integration knowledge. Replace the structure around it.

## 2. What we did well

### 2.1 Coordinate-frame reasoning is explicit and unusually careful

`CTRESwerveDrivetrain.java` documents the difference between robot-relative and field-relative requests, and the vision code documents how latency and covariance interact. This is the kind of comment that prevents a whole class of autonomous bugs, and it is better than most public team code.

Evidence: `src/main/java/frc/robot/Drivetrain/CTRESwerveDrivetrain.java`, `src/main/java/frc/robot/Vision.java`.

### 2.2 Vision covariance scales with distance and tag count

Rather than a fixed measurement standard deviation, the 2026 vision code scales trust by range and tag count. That is the right instinct and matches what 1678/9496/5940 do with more elaborate models.

Evidence: `Vision.java` (standard-deviation selection and rejection path).

### 2.3 The logging raw material is already present

The robot starts WPILib `DataLogManager`, URCL, DogLog (WPILOG + NT mirror), CTRE `SignalLogger`, and a custom `NetworkTableLogger`. Any one of these would be enough for a drive-only robot. The fact that all five exist means the team has already solved the hard part of **capturing** data; the remaining work is choosing one path and replaying it.

Evidence: `Robot.java:133-151`, `Robot.java:111`, `Robot.java:180-229`.

### 2.4 CTRE swerve integration is functional and complete

Four modules, Pigeon 2, `SwerveRequest` composition, SysId routines, PathPlanner configuration, and a `Field2d` all work together. The 2027 chassis can reuse this integration knowledge directly.

Evidence: `CTRESwerveDrivetrain.java`, `Autos.java`, `Drivetrain/Telemetry.java`.

### 2.5 Driver feedback is treated as a feature

The 2026 code has working LED fire patterns, Elastic notifications, and a real dashboard layout. Many teams stop at a `Field2d`. Glitch did not.

Evidence: 2026 LED subsystem and `src/main/deploy/elastic-layout.json`.

### 2.6 CI and tests exist

`.github/workflows/ci.yml` runs `./gradlew build` and uploads test results; it also JSON-lints deploy files. There are two test classes with **nine** `@Test` methods total (`ProjectileSolverTest`, `FieldGeometryTest`). That is not enough, but it is more than 5940 and 9496 ship.

### 2.7 The Agent Context repo is a real differentiator

A written, AI-facing source of truth with known issues, units/frames, and an off-season plan is not something the four benchmark teams have. It is the reason this report can distinguish facts from guesses.

## 3. What we should improve

| # | Problem | Severity | Evidence | Fix |
|---|---|---|---|---|
| 1 | `Robot.java` is a 304-line monolith that mixes lifecycle, field geometry, shooter math, and subsystem wiring | High | `Robot.java` (imports and methods) | Move to `RobotContainer` + command-based or explicit state objects; keep `Robot` to lifecycle only |
| 2 | Five logging surfaces with overlapping data | High | `Robot.java:111`, `133-151`; `Drivetrain/Telemetry.java` | Pick DogLog + DataLog for replay; keep `SignalLogger` only for raw motor capture; delete `NetworkTableLogger` |
| 3 | No IO seam, no replay, no desktop swerve sim | High | `CTRESwerveDrivetrain` talks to hardware directly; `includeDesktopSupport = true` but no IO implementations | Add `DriveIO`/`VisionIO` with Real/Sim; replay afterward |
| 4 | Tests do not gate the build | High | `build.gradle` test config; CI runs `build` but there is no coverage/format gate | Make `build` depend on `test`; add Spotless; add a coverage floor for pure logic |
| 5 | Static mutable state (`Robot.firing`, static field geometry) | High | `Robot.java` static fields and `firing` references | Replace with a `RobotState`/`Superstructure` object; inject it |
| 6 | No command-based organization; auto and teleop logic are interleaved | Medium | `Robot.java`, `Autos.java` | `RobotContainer`, subsystem default commands, `AutoChooser` |
| 7 | No Spotless/Checkstyle, despite README-level formatting claims | Medium | `build.gradle` has no `spotless` block | Add Spotless + `spotlessCheck` in CI |
| 8 | Auto selection has no start-pose validation | Medium | `Autos.java` selects a command; no check that the robot is on the path start | Log chosen auto + expected start pose; reject if mismatch |
| 9 | Vision has no shared policy object; rejection logic is spread through the pipeline | Medium | `Vision.java` | Extract `VisionPolicy` with ambiguity/distance/tilt rules and unit tests |
| 10 | CANdle code streams commands per frame instead of using on-device animations | Low (fixed on 2027) | 2026 LED code / 2027 original `CandleLEDs` | Use `ColorFlowAnimation`/`TwinkleAnimation` and only write on state change |
| 11 | Dead/legacy code and stale comments remain in the tree | Low | Commented-out variants, `[OLD]`/`[NEW]` log keys (`Robot.java:211-212`) | Delete or move to `docs/legacy/`; never log two versions of the same value |
| 12 | Robot identity and tuning values are compile-time or scattered | Medium | Constants classes and build-time config | One `RobotConfig`, `LoggedTunableNumber`, and a PR check for non-default values |

## 4. What to keep vs retire for 2027

**Keep**

- The coordinate-frame comments and the habit of documenting units.
- CTRE `SwerveDrivetrain` + Pigeon 2 + `Field2d` integration.
- Distance-scaled vision trust and rejection instincts.
- DogLog + WPILog capture.
- SysId routines and the PathPlanner setup.
- CI and the two pure-logic tests.
- Elastic as the driver surface.

**Retire**

- `NetworkTableLogger` and duplicate NT logging.
- The monolithic `Robot` and static `firing` state.
- Per-frame CANdle pixel streaming.
- `[OLD]`/`[NEW]` dual logging and commented-out code paths.
- Any "it builds, so it works" assumption. The verification bar is a named human on the robot.

## 5. Top 10 actions before kickoff

1. Create `RobotState` and make pose/velocity/vision flow through it.
2. Add `DriveIO`/`VisionIO` interfaces with sim implementations.
3. Collapse logging to DogLog + DataLog; add `LoggedTracer` timing.
4. Move lifecycle wiring into `RobotContainer`.
5. Add `LoggedCommand` so every auto step is named.
6. Add `VisionPolicy` and tests for rejection/ambiguity.
7. Add start-pose validation to auto selection.
8. Make `./gradlew build` run tests and Spotless.
9. Convert CANdle to native animations with state-change writes.
10. Write a one-page "robot map" (device → bus → ID → class → log key) for prompt accuracy and pit debugging.

## 6. Honest comparison

Against the four benchmark teams, Glitch 2026 is:

- **Ahead** on written context, coordinate-frame documentation, and driver feedback.
- **At parity** on CTRE swerve integration and vision ambition.
- **Behind** on architecture (no IO/replay), logging discipline (five stacks), test enforcement (nine tests, no format gate), and auto structure (no start-pose validation).

The gap is not talent. It is **enforcement**: the team knows what good looks like but has not made the good path the only path. The 2027 chassis branch is the chance to make it the only path.
