# 2027 vendordeps

This branch intentionally ships **no vendordep JSON files**. The 2027 vendor JSON formats and
versions are still changing, and a stale JSON breaks the whole Gradle configuration.

Install these through the WPILib 2027 VS Code vendor-library manager:

1. Phoenix 6 `26.70.0-alpha-2` (SystemCore; not roboRIO-compatible).
2. PhotonLib 2027 alpha (2 or newer).
3. Do not install the 2026 `WPILibNewCommands` vendordep.

After installation, commit the generated JSON files so the branch is reproducible. Record the
exact versions in `docs/syscore-migration.md`.
