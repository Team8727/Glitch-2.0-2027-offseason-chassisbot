# Testing and learning

The purpose of a failed check is to identify a behavior or assumption to examine.
It is feedback about the change, not a rating of the member who submitted it.

## One build definition, locally and in GitHub

The baseline is adapted from the competition repo
`Team8727/Glitch-2.0-2026-chassisbot` at commit `a55e5ed`.

| Ingredient | Authoritative file | Current selection |
| --- | --- | --- |
| Java major version | `.java-version`, read by Gradle and CI | 17 |
| Gradle | `gradle/wrapper/gradle-wrapper.properties` | 8.11, checksum verified |
| GradleRIO / WPILib | `build.gradle` | 2026.2.1 |
| CTRE Phoenix | `vendordeps/Phoenix6-26.1.1.json` | 26.1.1 |
| REV | `vendordeps/REVLib.json` | 2026.0.2 (used by GlitchLib) |
| PhotonVision | `vendordeps/photonlib.json` | v2026.2.2 (used by GlitchLib) |
| Command framework | `vendordeps/WPILibNewCommands.json` | WPILib-selected version |
| GlitchLib | Git submodule entry | `2832afe35dd10c3b9226e48d82cd45e3a06a962a` |
| JUnit | `build.gradle` | 5.10.1 |

The full pinned GlitchLib source and its tests are compiled without modifying
the library. Its motor and vision classes require REV and PhotonVision even
though the robot baseline currently uses only its LED patterns. Unused
competition dependencies (PathPlanner, Studica, URCL) are not carried over.

After pulling a dependency change, run `git submodule update --init --recursive`
and `./gradlew build` while online. Do not use `git submodule update --remote`:
that would move the library away from the reviewed revision. Do not rely on
manually copied jars or uncommitted local Maven packages.

Java's major version is shared; this does not force identical JDK patch versions
or operating systems. `./gradlew --version` reports the Gradle runtime, and Gradle
selects a Java 17 toolchain for compilation and tests. If it cannot find Java 17,
use the JDK supplied by WPILib 2026 or install Java 17. A Linux CI pass does not
prove every vendor native library works on every Mac architecture.

## Everyday workflow

1. Start a task branch from the shared `main` using
   `<developer-name>/<change-theme>`, for example `charlie/led-timeout-tests`
   or `griffin/ci-reports`. Use the responsible member's name or GitHub handle,
   including for AI-assisted work. Use lowercase letters, numbers, and hyphens
   within each part, and a short, specific theme. Create one branch per task.
2. Write the expected behavior and a failure case in plain language. Ask Kilo
   for tests that demonstrate that contract, then inspect the assertions.
3. Run a focused test while working:

   ```sh
   ./gradlew test --tests 'frc.robot.Subsystems.LEDSubsystemTest'
   ```

4. Before proposing the change, run the full check:

   ```sh
   ./gradlew build
   ```

5. Push and open a PR. Read the Actions summary and complete the PR's evidence,
   learning, and hardware-limit sections. Be able to explain an assertion and
   predict a change that would make it fail.

`build` runs both robot and library tests and rejects a result with no executed
robot tests. A focused `test --tests ...` run is useful feedback but is not a
replacement for the full build. Local HTML results are at
`build/reports/tests/test/index.html`.

The naming pattern is a team convention, not a GitHub requirement. It makes
ownership and purpose visible. GitHub's
[branch-name guidance](https://docs.github.com/en/get-started/using-git/dealing-with-special-characters-in-branch-and-tag-names)
supports using simple characters to avoid shell-escaping problems. Avoid a
standalone branch named only after a member: `charlie` and `charlie/topic`
can conflict in Git's reference namespace. Existing branches need not be
renamed just to adopt this convention for new work.

## Initial behavior contracts

| Area | Expected behavior | Why it matters |
| --- | --- | --- |
| Mode indication | Disabled selects green; auto/teleop select blue | Mode changes stay understandable |
| Override | Active before expiry, base restored at the boundary | Temporary signals must not become permanent |
| Clock initialization | A zero initial clock value is valid | Desktop tests and startup must track elapsed time correctly |
| Mode transitions | A new mode cancels the prior override | Old state must not override the current mode |
| Test mode | A ten-second scan returns to the current base | Bench checks do not permanently replace status |
| Null patterns | Render off | Missing patterns must not crash periodic rendering |
| Output interval | A frame is written at most once per 200 ms | Bounds requested CAN writes; not a timing benchmark |
| Output disabled | Blank once and suppress normal writes; timers continue | Expired overrides do not return on re-enable |
| Device failure | Stop the failed frame, retry at the normal interval, log once per episode | Avoid a retry/log flood; report a new fault after recovery |
| Pixel addressing | External strip pixel 0 maps to CANdle index 8; 60 pixels end at 67 | Avoid writing onboard LEDs or missing the strip end |

The tests use a fake LED output and clock, with WPILib desktop HAL initialized.
They execute real rendering and mode logic but do not create a physical CANdle.
Random fire-animation frames are not golden-image tested. Time advances without
sleeping. The library's own tests run alongside these tests.

## Reading CI feedback

The required-check candidate is **Build and behavioral tests**. The summary
reports build status and passed/failed/skipped counts, distinguishes robot tests
from library tests, and includes assertion messages plus reproduction commands.
Download `verification-reports` for the HTML report and JUnit XML, including on
failure. Reports are retained for 14 days.

- **Setup/dependency failure:** inspect the first failed step. No test report
  means behavior was not checked; it does not mean the robot behavior failed.
- **Compilation failure:** compare the exact compiler error against the pinned
  API. Do not invent vendor classes or upgrade libraries just to silence it.
- **Behavioral failure:** compare expected and actual results against the
  requirement. Fix the code, or explicitly discuss why the requirement changed.
- **Unexpected skips/no robot tests:** restore meaningful tests. The shared
  library passing alone is insufficient.

For a bug fix, first run the new regression test against the old behavior and
record its failure; then apply the fix and rerun it. Do not weaken expectations
simply to obtain green checks. A passing test is evidence for its contract, not
proof of optimal code or better match performance.

## What CI does not establish

Before deploying this LED-only baseline, verify on the bench: CAN ID 14, external
strip length 60, RGB order, brightness, power wiring, configuration success,
first/last pixel addressing, mode transitions, and disconnect/recovery behavior.
The adapter enables the 5V rail and sets VBat output on; match this to the actual
strip wiring. The 200 ms write interval is a traffic policy, not a guarantee of
photosensitivity safety. CI does not deploy or score simulated matches.

The baseline still uses per-pixel writes, and a failed blanking request has no
dedicated retry while output remains disabled. Treat these as future measured
improvement opportunities, with explicit requirements and tests before changing
behavior. Invalid override-duration policy also remains a future contract decision.

## Repository rollout

After this PR is reviewed and merged, configure a `main` ruleset requiring a PR,
a reviewer, and **Build and behavioral tests**. Repository protection settings
are not activated by committing this workflow. Confirm the exact check name on
a successful PR run before selecting it.

Existing exercise branches must merge/rebase the updated baseline to receive
the workflow and dependency setup. Branches using a different SystemCore
toolchain need their own deliberate configuration; do not silently force them
onto the roboRIO setup. A push checks the pushed tip, not every intermediate
commit in a multi-commit push. Push and PR checks both run intentionally.
