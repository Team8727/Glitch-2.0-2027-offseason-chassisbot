# MK5N Swerve Implementation Plan (Final)

## Hardware Summary
| Spec | Value |
|------|-------|
| Module | MK5N (Team 8727 custom, SDS-style gearing) |
| Drive motor | NEO (SPARK MAX, velocity PID mode) |
| Steer motor | NEO 550 (SPARK MAX, position PID mode) |
| Absolute encoder | CTRE CANcoder (Phoenix 6) |
| Gyro | Pigeon 2 (CTRE Phoenix 6) |
| Drive ratio | 7.01:1 |
| Steer ratio | 26.09:1 |
| Wheel diameter | 4" (0.1016 m) |
| Chassis | 26" × 26" (0.6604 m track/wheelbase) |
| CAN bus | "rio" (default) |
| Language | Java, WPILib 2026 |
| Framework | Command-based, SPARK MAX + Phoenix 6 |
| Scope | Field-oriented joystick drive + heading lock |

## Conventions (from AGENTS.md + 2026 chassisbot)
- **Branch**: work on branch `Milo`, never `main`
- **Packages**: `frc.robot.Drivetrain` for swerve, `frc.robot.Subsystems` for other subsystems, `frc.robot.Commands` for commands
- **GlitchLib**: deferred — NOT included now. Will be added as submodule later when LED subsystem is ported. Swerve code does not use GlitchLib.
- **Build**: WPILib GradleRIO `2026.2.1`, Java 17

---

## Task 1 — Project scaffolding
Files: `build.gradle`, `settings.gradle`, `gradlew`/`gradlew.bat`, `gradle/`, `.wpilib/wpilib_preferences.json`, `vendordeps/`

1. Create GradleRIO 2026.2.1 Java project (template from WPILib VS Code, or manually)
2. Set `includeDesktopSupport = true`
3. **Vendordeps** (place JSON files in `vendordeps/`):
   - `Phoenix6-26.1.1.json` — CANcoder + Pigeon 2 API
   - `REVLib.json` — SPARK MAX API (2026 version)
   - `WPILibNewCommands.json` (standard)
   (PathPlannerLib NOT included now — add later when autonomous paths are needed)
4. Create directory structure:
   ```
   src/main/java/frc/robot/
     Drivetrain/
     Commands/
     Robot.java
     RobotContainer.java
     Main.java
     Constants.java
   ```

## Task 2 — Constants
File: `src/main/java/frc/robot/Constants.java`

- `CAN_BUS_NAME = "rio"`
- `PIGEON_ID = 2`
- `DRIVE_RATIO = 7.01`
- `STEER_RATIO = 26.09`
- `WHEEL_DIAMETER_METERS = 0.1016`
- `WHEEL_CIRCUMFERENCE = WHEEL_DIAMETER_METERS * Math.PI`
- `DRIVE_MOTOR_FREE_SPEED_RPM = 5676` (NEO) — used for max theoretical speed
- `MAX_LINEAR_SPEED_MPS = DRIVE_MOTOR_FREE_SPEED_RPM / DRIVE_RATIO * WHEEL_CIRCUMFERENCE / 60`
- `MAX_ANGULAR_SPEED_RADPS = 3 * Math.PI` (tuned per chassis)
- Module positions as `Translation2d`: (±0.3302, ±0.3302)
- `SwerveDriveKinematics` instance
- Module CAN IDs: placeholder constants per module (driveID, steerID, cancoderID, cancoderOffset)
- **Per-module invert flags** (from 2026 chassisbot convention):
  - `kInvertLeftSide = false` — modules on the left side (FL, BL) use positive drive direction as forward
  - `kInvertRightSide = true` — modules on the right side (FR, BR) drive motors are inverted so positive = forward
  - Per-module `kSteerMotorInverted = false` — NEO 550 direction matches CANcoder positive direction
  - Per-module `kEncoderInverted = false` — CANcoder direction matches steering wheel rotation
  (These are tuned during initial bringup if wheels go the wrong way.)
- Joystick: deadband 0.1, drive/rotation speed multipliers
- SPARK MAX PID slots: `kDriveP`, `kDriveI`, `kDriveD`, `kDriveFF` (placeholders)
- Steer PID: `kSteerP`, `kSteerI`, `kSteerD` (placeholders)

## Task 3 — SwerveModule class
File: `src/main/java/frc/robot/Drivetrain/SwerveModule.java`

**Fields**: `CANSparkMax driveMotor, CANSparkMax steerMotor, CANcoder cancoder`

**Constructor**(int driveID, int steerID, int cancoderID, double cancoderOffset, boolean driveInverted, boolean steerInverted, boolean encoderInverted):
- Drive SPARK MAX: `MotorType.kBrushless`, restore factory defaults, set idle to `kBrake`, current limit `60A`, `CANSparkMax.ControlType.kVelocity`, slot 0 with placeholder PID, `setInverted(driveInverted)`
- Steer SPARK MAX: `MotorType.kBrushless`, restore factory defaults, set idle to `kBrake`, current limit `20A`, `CANSparkMax.ControlType.kPosition`, slot 0 with placeholder PID, `setInverted(steerInverted)`
- Set status frame periods: Status 0 to 10ms, Status 1 to 20ms, Status 2 to 10ms on both SPARK MAX for fast feedback
- Drive encoder: set `setPositionConversionFactor(WHEEL_CIRCUMFERENCE / DRIVE_RATIO)` and `setVelocityConversionFactor(WHEEL_CIRCUMFERENCE / DRIVE_RATIO / 60)` so `getPosition()` returns meters and `getVelocity()` returns m/s
- Steer encoder: keep at factor = 1.0 (motor rotations)
- CANcoder: `new CANcoder(cancoderID, CAN_BUS_NAME)`, `AbsoluteSensorRange` set to `Signed_PlusMinusHalf`, `setSensorDirection` per `encoderInverted` flag

**initSteerEncoder()** — call once after boot:
- Read CANcoder absolute position (in rotations, -0.5 to 0.5)
- Apply offset: `cancoderRot = (cancoderRot + 0.5) - 0.5` for wrapping, then `(cancoderRot - cancoderOffset + 0.5) % 1.0 - 0.5` or similar
- Convert to motor rotations: `motorRotations = cancoderRot * STEER_RATIO`
- `steerMotor.getEncoder().setPosition(motorRotations)`

**setDesiredState(SwerveModuleState state)**:
- Optimize: `SwerveModuleState.optimize(state, getCurrentAngle())`
- Drive: convert m/s → NEO RPM → set SPARK MAX reference (velocity)
  - `wheelRPM = state.speedMetersPerSecond / WHEEL_CIRCUMFERENCE * 60`
  - `motorRPM = wheelRPM * DRIVE_RATIO`
  - `driveMotor.getPIDController().setReference(motorRPM, CANSparkMax.ControlType.kVelocity)`
- Steer: convert target angle → motor rotations → set SPARK MAX reference (position)
  - `targetMotorRotations = state.angle.getRadians() / (2 * Math.PI) * STEER_RATIO`
  - `steerMotor.getPIDController().setReference(targetMotorRotations, CANSparkMax.ControlType.kPosition)`
- Store optimized state for `getState()` / `getPosition()`

**getState()**: returns `SwerveModuleState` from encoder feedback

**getPosition()**: returns `SwerveModulePosition` (drive distance + angle)

**stop()**: stops both motors

## Task 4 — Drivetrain subsystem
File: `src/main/java/frc/robot/Drivetrain/Drivetrain.java`

Extends `SubsystemBase`:
- **Constructor**: creates 4 `SwerveModule` instances, calls `initSteerEncoder()` on each, creates `SwerveDriveOdometry`, creates Pigeon 2
- **Pigeon 2**: `new Pigeon2(PIGEON_ID, CAN_BUS_NAME)`, get yaw via `getYaw().getValueAsDouble()`
- **`drive(double xSpeed, double ySpeed, double rot, boolean fieldRelative)`**:
  - Convert to `ChassisSpeeds` (field-relative if true using gyro angle)
  - Convert to `SwerveModuleState[]` via kinematics
  - Apply `SwerveModuleState.desaturateWheelSpeeds()`
  - Call `setDesiredState()` on each module
- **`periodic()`**: update odometry (`odometry.update(getGyroRotation(), getModulePositions())`), publish pose/module states to SmartDashboard
- **`resetGyro()`**: `pigeon.setYaw(0)`
- **`resetOdometry(Pose2d pose)`**: reset gyro + odometry

**Heading lock helper** (PID):
- `ProfiledPIDController` or simple `PIDController` for rotation
- When rotation input is within deadband, use PID to hold last heading
- Expose as a `Supplier<Double>` for the drive command

## Task 5 — SwerveDriveCommand
File: `src/main/java/frc/robot/Commands/SwerveDriveCommand.java`

Default command for Drivetrain:
- **Constructor**(Drivetrain, xSupplier, ySupplier, rotSupplier, fieldRelativeSupplier)
- **execute()**: read suppliers, apply deadband, call `drivetrain.drive()`
- **Heading hold**: when rot is neutral, PID-lock to the angle the robot was at when the stick entered deadband
- Marked as default command for Drivetrain in RobotContainer

## Task 6 — RobotContainer
File: `src/main/java/frc/robot/RobotContainer.java`

- Create `Joystick` (driver port 0, operator port 1)
- Create `Drivetrain`
- Configure default command: `new SwerveDriveCommand(drivetrain, ...)`
- Button bindings:
  - **A button**: `drivetrain.resetGyro()` (zero heading)
  - **Start button**: toggle field-relative

## Task 7 — Robot
File: `src/main/java/frc/robot/Robot.java`

Standard `TimedRobot` with `RobotContainer`:
- `robotInit()` → `robotContainer = new RobotContainer()`
- `teleopPeriodic()` / `autonomousPeriodic()` → `CommandScheduler.getInstance().run()`
- `autonomousInit()` → placeholder empty command
- `disabledInit()` / `disabledPeriodic()` → optional LED disabled pattern (future)

## Task 8 — Build & Validate
1. `./gradlew build` — compile clean
2. Deploy, connect SmartDashboard
3. Verify CANcoder readings match physical wheel angles (manually rotate each module)
4. Verify Pigeon 2 yaw responds to robot rotation
5. Push robot in X/Y — confirm field-relative drive moves robot in the correct direction
6. Release rotation stick — confirm heading lock holds angle
7. Adjust PID gains for drive velocity and steer position until drivetrain feels responsive and stable

## Open Items (user must fill, no code blocker)
- Module CAN IDs (drive, steer, CANcoder) for all 4 modules
- CANcoder absolute offset per module (measure with wheels straight, record the reported absolute position)
- Drive velocity PID (kP, kI, kD, kFF) — start with ~0.0001 kP, 0.0001 kFF for NEO
- Steer position PID — start with ~0.01 kP

## Out of scope (add later if needed)
- PathPlanner auto paths
- Vision integration (photonlib)
- SysId characterization
- GlitchLib LED subsystem integration