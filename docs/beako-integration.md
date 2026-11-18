# Beako-RIO integration (review draft)

This branch integrates the named exercise branch `Beako-RIO` (`622c891`) into the
shared baseline `main` (`811f1a2`). The two branches have unrelated commit
histories, so the change was assembled deliberately on a new branch rather than
by merging or rebasing either side. `main` and `Beako-RIO` are unmodified.

## What is being proposed

The 2027 offseason chassis exercise adds:

| Area | Source | Notes |
| --- | --- | --- |
| CTRE swerve drivetrain | `Drivetrain/CTRESwerveDrivetrain`, `Drivetrain/TunerConstants` | SDS MK5n modules, Pigeon 2, CANivore |
| Driver bindings | `RobotContainer` | Field/robot-centric toggle, brake, pose reset |
| PhotonVision fusion | `Vision/*` | FrontL/FrontR cameras, measurement filtering |
| PathPlanner autos | `Autos/Autos`, `src/main/deploy/pathplanner` | `DriveForward`, `Circle`, taxi, pathfind |
| Telemetry | `Robot`, `DrivetrainTelemetry` | DogLog plus Elastic layout |
| Status LEDs | `Subsystems/StatusLEDs` | On-device CANdle animations |
| Steer calibration | `Commands/SteerOffsetCalibration` | Dashboard-triggered |

## What was preserved from main

These files are byte-identical to `origin/main`:

- `.github/workflows/verify.yml` and `tools/ci_summary.py` (CI and reporting)
- `gradlew`, `gradlew.bat`, `gradle/wrapper/*` (pinned wrapper, checksum verified)
- `.java-version`, the Java 17 toolchain and `verifyRobotTests` guard in `build.gradle`
- `.gitmodules` and the pinned `GlitchLib` submodule (`2832afe35dd10c3b9226e48d82cd45e3a06a962a`)
- `Subsystems/CandleLEDs`, `LEDSubsystem`, `LedOutput`, `PhoenixLedOutput`
- `src/test/java/frc/robot/Subsystems/LEDSubsystemTest` and `PhoenixLedOutputTest`
- `AGENTS.md`, `docs/testing.md` conventions, and the pull request template

No test was deleted, skipped, or weakened. Beako's test sources were copied
verbatim, as were its subsystem sources and deploy assets.

## Runtime and LED decisions

`Robot` runs the **Beako-RIO lifecycle**: it constructs a `RobotContainer`, owns
the command scheduler loop and mode transitions, starts DogLog, and schedules the
PathPlanner pathfinding warmup. `Main` still starts `Robot`, so the WPILib entry
point is unchanged in behavior.

Main's `LEDSubsystem` and Beako's `StatusLEDs` both drive the robot's single
CANdle strip. Only **one** controller is instantiated at runtime: `RobotContainer`
constructs `StatusLEDs`. `LEDSubsystem` remains compiled and covered by its 11
baseline tests, but nothing in the runtime path calls `LEDSubsystem.getInstance()`,
so no second `CANdle` handle is created for the same physical device.

This selection means the strip shows Beako's on-device status animations
(green disabled, blue auto/teleop, cyan sparkle when vision is active, red for
low battery or fault) rather than main's software-rendered green/blue fire.

**Unresolved:** the two branches disagree on the strip hardware. Main's LED
baseline documents CANdle ID **14** with **60** pixels; Beako's
[robot configuration](robot-config.md) documents ID **49** with **88** pixels on
native roboRIO CAN. The integration follows Beako's values because the imported
drivetrain and wiring notes target the 2027 chassis. Confirm the actual device ID,
pixel count, and RGB order before enabling the robot.

## Dependency changes

No new Gradle coordinates were added. `build.gradle`, `settings.gradle`, and the
wrapper are unchanged. Beako's two vendor declarations were added:

- `vendordeps/DogLog.json` (telemetry)
- `vendordeps/PathplannerLib-2026.1.2.json` (autos)

`vendordeps/Phoenix6-26.1.1.json`, `REVLib.json`, `WPILibNewCommands.json`, and
`photonlib.json` are whatever `main` already pins; Beako's copies were not taken,
so the pinned Phoenix/PhotonVision/REV versions are unchanged. `GlitchLib` still
supplies the REV and PhotonVision classes its own sources need.

## Test evidence

```sh
git submodule update --init --recursive
./gradlew build
```

The suite is one compilation and one Gradle test run:

| Suite | Tests | Result |
| --- | ---: | --- |
| `main` robot baseline | 14 | 14 passed |
| Pinned GlitchLib | 17 | 17 passed |
| `Beako-RIO` exercise | 17 | 17 passed |
| **Total** | **48** | **48 passed, 0 failed, 0 skipped, 0 errored** |

Counts come from `build/test-results/test/TEST-*.xml`, not from the build status
alone. Per class: `LEDSubsystemTest` 11, `PhoenixLedOutputTest` 3,
`PathPlannerFilesTest` 3, `TunerConstantsTest` 3, `StatusLEDsTest` 5,
`VisionMeasurementFilterTest` 6, and GlitchLib's `LEDTests` 5, `PivotTest` 3,
`RollerTest` 5, `NetworkTableLoggerTest` 4.

## What this does not prove

Passing tests show that both codebases compile together under one dependency set
and that their existing assertions still hold. They do **not** prove robot
performance, wiring, or safe deployment. They add no new cross-subsystem tests:
nothing here exercises swerve, vision, and LEDs together, and nothing creates a
physical CANdle or moves a motor.

Bench checks still required:

- CANdle device ID, pixel count, RGB order, brightness, and 5V rail behavior
- All 12 module devices plus Pigeon 2 visible on the CANivore; CAN terminators
- Four encoder offsets and wheel radius measured and entered in `TunerConstants`
- Camera mounts measured and entered in `VisionConstants`; PhotonVision reachable
- Auto chooser selection and `DriveForward`/`Circle` on blocks, then on carpet
- Brownout behavior and the low-battery LED threshold under load
- Driver controller button mapping with the real joystick
