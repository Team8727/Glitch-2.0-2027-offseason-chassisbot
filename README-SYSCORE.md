# Beako-Syscore — Experimental WPILib 2027 / SystemCore Branch

**Status: source complete, build unverified.** This branch is intentionally separated from
`Beako-RIO` because WPILib 2027 is still alpha and its vendor libraries are alpha too.

## Why it is unverified

- WPILib 2027.0.0-alpha-8 (Oct 30, 2026) is installed nowhere on this Mac; only WPILib 2026 exists.
- 2027 requires **Java 25**; the 2026 toolchain ships Java 17.
- Phoenix 6 26.70.0-alpha-2 supports SystemCore/2027 alpha 7 and **does not support the roboRIO**.
- PhotonLib 2027 is alpha; PathPlanner has no 2027 release yet.
- No SystemCore hardware was available for this branch.

Do not put this branch on a robot until every item in `docs/syscore-migration.md` is checked.

## What is here

```text
src/main/java/frc/robot/
  Main.java                 RobotBase entry point
  Robot.java                TimedRobot lifecycle, drive-only
  RobotContainer.java       intentionally empty
  drivetrain/
    TunerConstants.java     2027 CANBus + swerve constants
    Drive.java              thin swerve wrapper
  vision/Vision.java        two PhotonLib cameras
  leds/CandleLEDs.java      native CANdle blue-flame animations
  autos/AutoChooser.java    Selectable + timed taxi fallback
  telemetry/Telemetry.java  explicit /Elastic NT4 topics
src/main/deploy/
  elastic-layout.json       same topic design as Beako-RIO
docs/
  syscore-migration.md      step-by-step completion guide
```

## Verified facts used

- Plugin id is `org.wpilib.GradleRIO` (GradleRIO 2027+).
- Java 25 and C++23 are required for 2027.
- `TimedRobot`, `Selectable`, `Tunables`, and `robotInit()` removal are documented 2027 changes.
- `SmartDashboard`, `SendableChooser`, and `Sendable` were replaced by Telemetry/Tunables.
- WPILib raw timestamps are nanoseconds in 2027.
- Phoenix 6 requires a `CANBus` object; SystemCore's default bus is `can_s1`.
- Phoenix 26.70 renamed the swerve network field to `Network` and accepts a `CANBus`.

## Next step

Follow `docs/syscore-migration.md`. The shortest safe path is to let the WPILib 2027 importer
regenerate the Gradle project, then copy this branch's `src/` and `deploy/` into it. The
importer knows the 2027 GradleRIO/deploy layout better than this hand-written build file.
