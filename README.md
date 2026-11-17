# Glitch 2.0 — 2027 offseason chassisbot

`main` is the shared baseline. Named exercise branches explore alternatives and
are not automatically part of this robot.

This branch proposes integrating the `Beako-RIO` 2027 chassis exercise — CTRE
swerve, PhotonVision fusion, PathPlanner autos, DogLog telemetry, and a CANdle
status strip — into the shared baseline. It keeps main's workflow, pinned
GlitchLib, and existing LED implementation and tests. Read
[the integration notes](docs/beako-integration.md) before enabling the robot:
several measurements are still unset and the LED device configuration must be
confirmed on hardware.

## Build and test

Use a WPILib 2026 development installation with Java 17, or a Java 17 JDK with
network access for Gradle to download the declared dependencies. VS Code is
not required for CI. The first build needs internet access.

```sh
git submodule update --init --recursive
./gradlew build
```

Windows uses `gradlew.bat`. GitHub Actions runs the same build on pushes to all
branches that contain the workflow, pull requests, merge groups, and manual runs.
CI never deploys code to the robot.

Read [Testing and learning](docs/testing.md) for targeted test commands,
understanding failures, dependency synchronization, and the remaining hardware
checks. See [AGENTS.md](AGENTS.md) for Kilo and other coding-agent instructions.
