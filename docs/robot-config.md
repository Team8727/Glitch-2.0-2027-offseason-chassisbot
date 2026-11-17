# 2027 Chassis Robot Configuration

> Fill every `TODO` by measuring the robot. Do not copy 2026 values onto the 2027 chassis.

## Drivetrain

| Item | Value | Source |
|---|---|---|
| Modules | 4x SDS MK5n | Prompt |
| Drive motor | Kraken X60 (Talon FX, integrated) | Confirmed with user; "X66" is not a CTRE product |
| Steer motor | Kraken X44 (Talon FX, integrated) | Prompt |
| Drive gear ratio | 7.03125:1 | `TunerConstants.kDriveGearRatio` |
| Steer gear ratio | 287:11 (26.09:1) | `TunerConstants.kSteerGearRatio` |
| Coupling ratio | 4.5 | `TunerConstants.kCoupleRatio` |
| Wheel radius | 2.0 in | Verify with calipers |
| Free speed at 12 V | 4.39 m/s | `TunerConstants.kSpeedAt12Volts` |
| Track width / wheelbase | 20.75 in x 20.75 in | `TunerConstants` module X/Y = ±10.375 in |
| CANivore name | `"Swerve Drive"` | `TunerConstants.kCANBus`; confirm in Tuner X |
| Pigeon 2 ID | 2 | `TunerConstants.kPigeonId` |

## CAN IDs

| Device | ID | Bus |
|---|---|---|
| Front Left drive | 12 | CANivore |
| Front Left steer | 11 | CANivore |
| Front Left CANcoder | 13 | CANivore |
| Front Right drive | 22 | CANivore |
| Front Right steer | 21 | CANivore |
| Front Right CANcoder | 23 | CANivore |
| Back Left drive | 32 | CANivore |
| Back Left steer | 31 | CANivore |
| Back Left CANcoder | 33 | CANivore |
| Back Right drive | 42 | CANivore |
| Back Right steer | 41 | CANivore |
| Back Right CANcoder | 43 | CANivore |
| Pigeon 2 | 2 | CANivore |
| CANdle | 49 | **roboRIO native CAN** |

The CANdle is deliberately native CAN. The 2026 robot's CANdle was unreachable on the
CANivore but worked with `new CANdle(49)`. Do not move it without re-testing.

## Encoder offsets

All four offset constants are `TODO` in `TunerConstants`:

```text
kFrontLeftEncoderOffset  = TODO
kFrontRightEncoderOffset = TODO
kBackLeftEncoderOffset   = TODO
kBackRightEncoderOffset  = TODO
```

Calibrate with Phoenix Tuner X (or the team's `SteerOffsetCalibration` routine) with the
wheels pointed straight forward, then paste the measured rotations.

## Cameras

| Camera | Mount | Transform | Pipeline |
|---|---|---|---|
| FrontL | USB to Rubik Pi 3 | TODO: x/y/z/roll/pitch/yaw | AprilTag, multi-tag preferred |
| FrontR | USB to Rubik Pi 3 | TODO: x/y/z/roll/pitch/yaw | AprilTag, multi-tag preferred |

- Rubik Pi is Ethernet to the radio.
- PhotonVision publishes over NT4; confirm the team number / NT server address.
- `VisionConstants` currently uses placeholder transforms until measured.

## CANdle / LEDs

| Item | Value |
|---|---|
| ID | 49 |
| LED count | 88 (2026 measured; verify the 2027 strip) |
| Strip type | RGB |
| Brightness | 0.6 |
| Animations | Blue color flow + cyan twinkle; green disabled; red low battery/fault |

## Dashboard

- Elastic layout: `src/main/deploy/elastic-layout.json`
- Tabs: `Driver`, `Diagnostics`, `Autos`
- All robot values publish under `/Elastic/...`; the auto chooser uses `/SmartDashboard/Auto choices`.

## Before the first drive

- [ ] Measure wheel radius and module positions.
- [ ] Calibrate all four encoder offsets.
- [ ] Confirm the CANivore network name and that all 12 module devices + Pigeon appear.
- [ ] Confirm the CANdle responds on native CAN with 88 LEDs.
- [ ] Confirm both cameras publish and the transforms are measured.
- [ ] Run `./gradlew build` and `./gradlew simulateJava`.
- [ ] Put the robot on blocks before enabling.
