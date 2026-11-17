# Beako-RIO workflow example

This isolated example runs the existing `Beako-RIO` exercise at `622c8910d5122e4609bd44cf139dc773c207589a`.
The original branch and main are unchanged. Robot source, tests, vendor
dependencies, and Gradle wrapper are unchanged. Only the workflow, Java version
selection for CI, report helper, and no-robot-tests guard were added.

Run `./gradlew build` using Java 17. For a focused run, use
`./gradlew test --tests 'frc.robot.Subsystems.StatusLEDsTest'`.

The existing tests cover LED status priorities, vision measurement filtering,
drivetrain configuration, and PathPlanner files. A green result only supports
those assertions; it does not prove hardware operation or match performance.

For failed builds, inspect the first compiler or dependency error. If tests run,
read the failed assertion and compare expected behavior with actual behavior.
Download the verification-reports artifact for HTML and XML results.

This branch is a demonstration, not a proposal to replace main's robot code.
