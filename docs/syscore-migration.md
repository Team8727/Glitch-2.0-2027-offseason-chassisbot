# SystemCore / WPILib 2027 Migration Guide

## Version matrix (pin these)

| Component | Version | Notes |
|---|---|---|
| WPILib | `2027.0.0-alpha-8` | Current alpha as of Oct 30, 2026; Beta 1 will break vendordeps |
| Java | 25 | Required |
| GradleRIO | `org.wpilib.GradleRIO` `2027.0.0-alpha-8` | Verify the plugin version in the plugin portal |
| Phoenix 6 | `26.70.0-alpha-2` | SystemCore only; requires 26.70.x device firmware |
| PhotonLib | 2027 alpha (2 or newer) | Install through the WPILib 2027 vendor manager |
| PathPlanner | none yet | Use the timed taxi in `AutoChooser`; add later |
| Elastic | 2027-compatible build | Must support Selectable; verify the install |
| SystemCore image | Alpha/Beta 14 or newer | Required by Phoenix 26.70 alpha |

## Step 1 — Install the toolchain

1. Install WPILib 2027.0.0-alpha-8 from the official WPILib installer.
2. Confirm `java -version` reports 25 inside the WPILib VS Code.
3. Create a throwaway 2027 TimedRobot project and build it. Do not start from this branch's
   `build.gradle`; prove the toolchain first.

## Step 2 — Let the importer regenerate the Gradle project

1. Open this branch in WPILib VS Code.
2. Run **WPILib: Import a WPILib 2026 Gradle project**.
3. Choose **SystemCore** as the hardware platform.
4. Let it regenerate `build.gradle`, the wrapper, `settings.gradle`, `gradle.properties`, and the
   deploy target.
5. Copy `src/main/java/frc/robot/**` and `src/main/deploy/**` from this branch into the generated
   project.
6. Build. Fix errors against the installed API, not against guesses in this document.

## Step 3 — Install vendor libraries

1. Phoenix 6 `26.70.0-alpha-2` for SystemCore.
2. PhotonLib 2027 alpha.
3. Do **not** install the 2026 WPILibNewCommands vendordep; 2027's command framework is different.

## Step 4 — API migration table

| 2026 | 2027 |
|---|---|
| `edu.wpi.first.wpilibj.TimedRobot` | `org.wpilib.framework.TimedRobot` |
| `edu.wpi.first.wpilibj.RobotBase` | `org.wpilib.framework.RobotBase` |
| `edu.wpi.first.wpilibj2.command.SubsystemBase` | Commands v3 mechanisms (or stay TimedRobot) |
| `ChassisSpeeds` | `ChassisVelocities` |
| `SmartDashboard.put*` | `Telemetry` / NT publishers |
| `SendableChooser` | `Selectable` |
| `robotInit()` | constructor |
| `MathUtil.clamp` | `Math.clamp` |
| microseconds timestamps | nanoseconds |
| `new CANdle(49)` | `new CANdle(49, canBus)` |
| `new CANBus("name")` | `new CANBus(CANPort)` or `new CANBus()` |
| `SwerveDrivetrainConstants.withCANBusName` | `.withNetwork(CANBus)` |
| `Utils.fpgaToCurrentTime` | removed; same timebase |

## Step 5 — Hardware verification order

1. **Power only.** Confirm SystemCore boots and the web dashboard is reachable.
2. **CAN bus.** Phoenix Tuner X should enumerate the SystemCore native bus (`can_s1`) and the
   CANivore if it is still in the chain. Confirm every module, Pigeon 2, and the CANdle.
3. **Firmware.** Update every Talon FX/Pigeon/CANdle to 26.70.x. Phoenix will refuse a mismatch.
4. **Drive on blocks.** Verify module order, inversion, and encoder offsets.
5. **CANdle.** Confirm 88 LEDs, RGB order, and the blue-flame animation.
6. **Cameras.** PhotonVision on the Rubik Pi, Ethernet to radio, NT4 topics visible.
7. **Elastic.** Confirm `/Elastic/...` topics and the layout tabs.
8. **Auto.** Run the timed taxi at low speed on carpet.

## Step 6 — Known unknowns in this branch

These are the places most likely to need edits after the first real compile:

- `org.wpilib.framework.RobotController.getBatteryVoltage()` package and method name.
- `org.wpilib.driverstation.Gamepad` constructor and axis/button methods.
- `SwerveDriveState.Speeds` type and field names after the ChassisVelocities rename.
- `SwerveRequest.FieldCentric.withDeadband` availability.
- `SwerveDrivetrain.periodic()` visibility when not using the command scheduler.
- `org.wpilib.apriltag.AprilTagFieldLayout` / REBUILT field loading. 2027 moved AprilTag fields
  into a vendordep and added a `Fields` class; do not reuse 2026 `AprilTagFields`.
- `PhotonPoseEstimator` 2027 alpha constructor/strategy names.
- `Selectable` generics and `Tunables.publish` signature.
- SystemCore deploy directory (`/home/systemcore/deploy` is assumed).
- CANivore support on SystemCore. Phoenix says native CAN and CANivores are functional; verify
  before removing or keeping the CANivore.

## Beta 1 upgrade checklist

When WPILib 2027 Beta 1 lands:

- [ ] Re-run the importer or update `org.wpilib.GradleRIO`.
- [ ] Update Phoenix and PhotonLib beta builds.
- [ ] Re-check every row in the API table.
- [ ] Rebuild; do not merge Beako-Syscore into Beako-RIO.
- [ ] Re-test on SystemCore hardware.
- [ ] Update `TunerConstants` if the swerve network API changes again.
