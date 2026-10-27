# Glitch 2.0 — 2027 Offseason Chassisbot

## Repository conventions

- **Never commit or work directly on `main`.** All work must happen on a branch
  named `Milo`. Create/switch to `Milo` (creating it from the latest `main` if it
  does not exist) before making any changes.
- Project conventions otherwise follow the team's 2026 chassisbot repo:
  `frc.robot.Subsystems` for subsystems, `frc.robot.Commands` for commands.
- **GlitchLib is no longer used in this project.** Do not reference it, restore
  the submodule, or add its sources back to the build.

## Build & test

The consuming WPILib 2026 project provides:
- `./gradlew build`
- `./gradlew :test`