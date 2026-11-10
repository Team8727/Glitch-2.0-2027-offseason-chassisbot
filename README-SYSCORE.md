# Glitch 2.0 Offseason Chassisbot — SystemCore (2027) Branch

This branch targets the **2027 System Core** robot controller with the
**WPILib 2027 alpha** toolchain (alpha era during the 2026 offseason).

> **Status: ALPHA PORT — NOT yet build-verified.**
> The WPILib 2027 alpha image and FIRST Driver Station Alpha are required to
> build/deploy. See [Build Requirements](#build-requirements) below.

## Hardware

- **Drive:** 4× SDS MK5n swerve modules
  - Drive motor: **Kraken X60** (`TalonFX_Integrated`), pinion R1 (7.03125:1)
  - Steer motor: **Kraken X44** (TalonFX), 287:11 steering ratio
  - Azimuth: CANcoder, FusedCANcoder feedback
- **CAN:** CTRE **CANivore** (USB to the controller), bus name `canivore`
- **Gyro:** Pigeon 2.0 (CAN ID 2)
- **Vision:** PhotonVision on a Rubik Pi; cameras **FrontL** / **FrontR** (USB);
  Pi is ethernet to the robot radio
- **LEDs:** CTRE **CANdle** (CAN ID 14), high-density RGB strip (300 LEDs),
  Lynk-style blue flame (`CustomFireAnimation`, Fire2012 port, MIT)
- **Pit/dashboard:** Elastic (remote layout via WebServer on port 5800),
  DogLog on the RIO branch, DataLogManager here

## What changed vs the RIO branch

| Area | `Fortnite-Pirate-Token-RIO` | `Fortnite-Pirate-Token-Syscore` (this branch) |
|------|------------------------------|------------------------------------------------|
| Toolchain | GradleRIO 2026.2.1 (RoboRIO) | GradleRIO2027 2027.0.0-alpha-2 (SystemCore) |
| WPILib | ~/wpilib/2026 | ~/wpilib/2027_alpha1 |
| PathPlanner | yes (2026.1.2) | **removed** (no 2027 alpha) |
| Autos | PathPlanner paths | simple drive-only command autos |
| DogLog | yes (2026.5.0) | removed (alpha snapshot mismatch) |
| Deploy dir | /home/lvuser | /home/systemcore |

## Build requirements

1. Install the **WPILib 2027 alpha** (VS Code extension/installer from
   `https://github.com/wpilibsuite/allwpilib/releases` — look for the newest
   `v2027.0.0-alpha-X`) so `~/wpilib/2027_alpha1` exists.
2. SystemCore image **14 or later** on the controller (see
   `wpilibsuite/SystemcoreTesting` repo for image + DS alpha requirements).
3. FIRST Driver Station **Alpha 7 or later**.
4. Vendor libs already pinned for 2027:
   - `Phoenix6-25.90.0-alpha-1.json`
   - `photonlib.json` → `v2027.0.0-alpha-2`
   - `ChoreoLib2027Alpha.json`
   - `WPILibNewCommands.json` (2027 era)

### Version pin notes

Alpha snapshots move fast. If a vendordep fails to resolve, check whether a
newer alpha of that library has been published and bump the `version` field in
its `.json` under `vendordeps/`.

## Deploy

Same as a normal WPILib project but the artifact deploys to the SystemCore
(`/home/systemcore/deploy`):

```bash
./gradlew deploy
```

## Autos

`Autos.java` builds drive-only routines from CTRE `SwerveRequest`s and timers
(no path library). When PathPlanner/Choreo ship 2027 alphas, port
`followPathFromStartPose` from the RIO branch and re-add the vendordep.