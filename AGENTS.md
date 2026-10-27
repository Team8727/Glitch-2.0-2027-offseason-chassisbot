# Glitch 2.0 — 2027 Offseason Chassisbot

## Repository conventions

- **Never commit or work directly on `main`.** All work must happen on a branch
  named `james`. Create/switch to `james` before making any changes.
- Project conventions otherwise follow the team's 2026 chassisbot repo:
  `frc.robot.Subsystems` for subsystems, `frc.robot.Commands` for commands,
  GlitchLib is consumed as a git submodule and left unmodified.

## Build & test

The consuming WPILib 2026 project provides:
- `./gradlew build`
- `./gradlew :test`