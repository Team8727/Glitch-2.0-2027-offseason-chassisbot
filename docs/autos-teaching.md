# Autos — Teaching Guide

This chassis is drive-only. That is an advantage: you can learn the whole auto pipeline without
a shooter, indexer, or game-piece state. The goal is one repeatable workflow you can reuse on the
2027 competition robot.

## 1. What an auto actually is

An auto is a **list of commands** with a known starting pose. Three pieces:

1. **A path** — where the robot drives (PathPlanner `.path` or Choreo `.traj`).
2. **Commands at the start/middle/end** — intake, score, wait, shoot.
3. **A selection** — the driver picks the routine before the match; the code validates it.

On this chassis, the only commands are drive commands and waits. That is enough to learn the
timing, pose, and selection problems that cause most auto failures.

## 2. The files

```text
src/main/deploy/pathplanner/
  settings.json          robot mass, wheelbase, max speed, etc.
  paths/DriveForward.path
  paths/Circle.path
src/main/java/frc/robot/Autos/Autos.java
src/main/java/frc/robot/Drivetrain/CTRESwerveDrivetrain.java
```

- `settings.json` must match the real robot. A wrong wheelbase or mass makes every path slow or
  overshoot.
- `DriveForward.path` is the simplest path: one straight segment.
- `Circle.path` tests rotation and centripetal behavior.
- `Autos.java` loads paths, flips them for the red alliance, and adds them to the chooser.

## 3. Build your first path in PathPlanner

1. Open PathPlanner (VS Code extension or desktop).
2. Open `src/main/deploy/pathplanner/settings.json`.
3. Create a new path named `TestForward`.
4. Place a start point at `(1.5, 4.0)` facing `0 deg` (toward the far side).
5. Add an end point at `(4.0, 4.0)` with the same heading.
6. Set max velocity to 2.0 m/s and max acceleration to 1.5 m/s².
7. Save. Confirm `TestForward.path` appears in `src/main/deploy/pathplanner/paths/`.
8. Add it to `Autos.java` with `addPathOption("TestForward", "TestForward")`.
9. Build and run in simulation. Watch the `Field2d` widget in Elastic/AdvantageScope.
10. Only then run it on blocks, then on carpet at low speed.

## 4. Why autos fail (and what to check)

| Symptom | Likely cause | Check |
|---|---|---|
| Robot drives the wrong direction immediately | Starting pose is wrong or the path was not alliance-flipped | Log the selected auto and the starting pose; compare with the path's start |
| Robot cuts a corner | Max acceleration/velocity too high for the carpet | Lower `settings.json` limits; check wheel radius |
| Robot oscillates at the end | PathPlanner PID too aggressive | Tune translation/rotation PIDs in `AutoBuilder` |
| Robot stops early | Command sequence ended or a requirement conflict | `Scheduler` tab in Elastic; check command names |
| Auto works on the left, fails on the right | Mirroring mistake | Use PathPlanner's built-in alliance flip, not hand math |
| Robot pose drifts before auto | Odometry not reset | Reset pose in `autonomousInit`; log it |

## 5. The one rule

**Log the selected auto name and the starting pose.** If those two values are not in the log,
you cannot debug an auto failure after a match.

The current `Autos` class publishes the chooser; add the selected name and pose to the
`/Elastic/Autos/...` topics when you extend it.

## 6. Practice plan

- Day 1: build and simulate `DriveForward`; change the end point; predict the time.
- Day 2: build a two-segment path with a turn; watch the heading graph.
- Day 3: add a 1-second wait and a second path; chain two paths.
- Day 4: run on blocks, then carpet at 1 m/s; compare log to simulation.
- Day 5: break it on purpose (wrong start pose) and practice diagnosing it from the log.

When you can do Day 5 without help, you are ready for competition autos.
