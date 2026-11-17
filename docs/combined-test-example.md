# Main + Beako-RIO combined test example

This example starts from shared main at `811f1a2` and adds the Beako-RIO
exercise at `622c891`. Neither original branch is modified.

## What is combined

- Main's build, workflow, pinned GlitchLib, LED implementation, and all 31 tests.
- Beako-RIO's robot source, deploy assets, and all 17 tests, unchanged.
- Beako's DogLog and PathPlanner vendor declarations. Shared Phoenix,
  PhotonVision, and WPILib command declarations match main exactly.

Main and Robot are resolved in favor of Beako's entry point/lifecycle for this
example. The baseline LED subsystem remains compiled and tested but is not
instantiated by Beako's Robot; Beako uses its own StatusLEDs. This is not a
validated combined hardware release or a proposal to replace main's runtime.

## How to read the run

One `./gradlew build` compiles both sets of code and runs both suites. The
additional **Verify baseline and exercise suites both ran** step checks the XML
reports for every expected test class and rejects missing or skipped tests.
The Actions summary shows **31 baseline + 17 exercise = 48 tests** separately.
The test sources are preserved byte for byte from their original branches.

A passing result means these existing assertions coexist under the shared
dependencies. It does not add new tests of interactions between the two LED
implementations, vision, and drivetrain, and does not validate robot wiring or
match performance. A compiler/dependency failure prevents behavior testing;
a test failure should be investigated against its documented requirement.

On a Java 17 development machine:

```sh
git submodule update --init --recursive
./gradlew build
python3 tools/verify_example_suite.py
```
