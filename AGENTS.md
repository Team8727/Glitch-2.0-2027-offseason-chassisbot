# Glitch 2.0 — 2027 Offseason Chassisbot

## Repository conventions

- **Never commit or work directly on `main`.** Start a descriptive task branch
  from the latest `main` (for example, `codex/ci-baseline`) and propose changes
  through a pull request. Named exercise branches are experiments, not the
  shared baseline; do not import their unrelated changes.
- Project conventions otherwise follow the team's 2026 chassisbot repo:
  `frc.robot.Subsystems` for subsystems, `frc.robot.Commands` for commands,
  GlitchLib is consumed as a git submodule and left unmodified.

## Build & test

Use the committed Gradle wrapper with Java 17. Local development and CI use
the same WPILib 2026 build configuration. Initialize the pinned library with
`git submodule update --init --recursive` before building.

- `./gradlew build`
- `./gradlew :test`

## Tests and learning

- Describe intended behavior and important failure cases before implementation.
- Add meaningful behavioral tests for code changes. For a bug fix, demonstrate
  that the regression test fails before the fix and passes afterward.
- Use fake hardware and controlled time; avoid sleeps and assertions on random
  animation frames. Assert observable results, including boundary conditions.
- Do not delete, skip, or weaken a test simply to get a passing build. Explain
  a requirement change explicitly when an expectation needs to change.
- Give assertions helpful messages: expected behavior, observed result, and
  why it matters. Explain what the test proves and what still needs hardware.
- Report the commands actually run and their outcomes. Compilation or zero
  discovered tests is not evidence that robot behavior was verified.
- Keep dependency versions in the repository. Update them through a PR with
  build/test evidence; never depend on a library present only on one laptop.
- See `docs/testing.md` for reproduction commands and the CI feedback format.
