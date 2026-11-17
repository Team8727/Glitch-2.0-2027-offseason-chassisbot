"""Ensure this demonstration actually runs both original test suites without skips."""

import os
from pathlib import Path
import xml.etree.ElementTree as ET

baseline = {
    "Glitch.Lib.BaseMechanisms.PivotTest": 3,
    "Glitch.Lib.BaseMechanisms.RollerTest": 5,
    "Glitch.Lib.LEDTests": 5,
    "Glitch.Lib.utilities.NetworkTableLoggerTest": 4,
    "frc.robot.Subsystems.LEDSubsystemTest": 11,
    "frc.robot.Subsystems.PhoenixLedOutputTest": 3,
}
exercise = {
    "frc.robot.Subsystems.StatusLEDsTest": 5,
    "frc.robot.Vision.VisionMeasurementFilterTest": 6,
    "frc.robot.Drivetrain.TunerConstantsTest": 3,
    "frc.robot.Autos.PathPlannerFilesTest": 3,
}
reports = {}
for path in Path("build/test-results/test").glob("TEST-*.xml"):
    suite = ET.parse(path).getroot()
    reports[suite.get("name")] = suite

lines = ["## Combined-suite verification", "", "| Suite | Passed | Expected |", "| --- | ---: | ---: |"]
problems = []
for label, expected in [("Main baseline (including GlitchLib)", baseline), ("Beako-RIO exercise", exercise)]:
    passed = 0
    for name, count in expected.items():
        suite = reports.get(name)
        if suite is None:
            problems.append(f"Missing test results: {name}")
            continue
        actual = int(suite.get("tests", 0))
        failed = sum(int(suite.get(key, 0)) for key in ["failures", "errors", "skipped"])
        passed += actual - failed
        if actual != count or failed:
            problems.append(f"{name}: expected {count} passing tests; found {actual} tests with {failed} failed/error/skipped")
    lines.append(f"| {label} | {passed} | {sum(expected.values())} |")

lines += ["", "Both suites share one compilation, dependency set, and Gradle test run. "
          "These are the original tests, not new cross-subsystem or hardware tests.", ""]
if problems:
    lines += ["Verification failed:", ""] + [f"- {problem}" for problem in problems]
else:
    lines += ["**All 31 baseline tests and all 17 exercise tests passed: 48 total.**"]
summary = "\n".join(lines) + "\n"
print(summary)
if os.environ.get("GITHUB_STEP_SUMMARY"):
    with open(os.environ["GITHUB_STEP_SUMMARY"], "a", encoding="utf-8") as output:
        output.write(summary)
if problems:
    raise SystemExit(1)
