"""Fail the build unless both original test suites ran without skips.

A green `./gradlew build` can still hide a suite that stopped being discovered,
or a test that was silently skipped. This checks the JUnit XML by class so the
shared baseline and the imported chassis exercise are both accounted for.
"""

import os
from pathlib import Path
import xml.etree.ElementTree as ET

BASELINE = {
    "Glitch.Lib.BaseMechanisms.PivotTest": 3,
    "Glitch.Lib.BaseMechanisms.RollerTest": 5,
    "Glitch.Lib.LEDTests": 5,
    "Glitch.Lib.utilities.NetworkTableLoggerTest": 4,
    "frc.robot.Subsystems.LEDSubsystemTest": 11,
    "frc.robot.Subsystems.PhoenixLedOutputTest": 3,
}

EXERCISE = {
    "frc.robot.Subsystems.StatusLEDsTest": 5,
    "frc.robot.Vision.VisionMeasurementFilterTest": 6,
    "frc.robot.Drivetrain.TunerConstantsTest": 3,
    "frc.robot.Autos.PathPlannerFilesTest": 3,
}

REPORT_DIR = Path("build/test-results/test")


def load_reports():
    reports = {}
    if not REPORT_DIR.is_dir():
        return reports
    for path in REPORT_DIR.glob("TEST-*.xml"):
        suite = ET.parse(path).getroot()
        reports[suite.get("name")] = suite
    return reports


def main():
    reports = load_reports()
    problems = []
    lines = [
        "## Suite verification",
        "",
        "| Suite | Passed | Expected |",
        "| --- | ---: | ---: |",
    ]

    for label, expected in (("Baseline (main + GlitchLib)", BASELINE),
                            ("Beako-RIO exercise", EXERCISE)):
        passed = 0
        for name, count in expected.items():
            suite = reports.get(name)
            if suite is None:
                problems.append(f"Missing test results: {name}")
                continue
            total = int(suite.get("tests", 0))
            blocked = sum(int(suite.get(key, 0))
                          for key in ("failures", "errors", "skipped"))
            passed += total - blocked
            if total != count or blocked:
                problems.append(
                    f"{name}: expected {count} passing tests; "
                    f"found {total} tests with {blocked} failed/error/skipped")
        lines.append(f"| {label} | {passed} | {sum(expected.values())} |")

    total_expected = sum(BASELINE.values()) + sum(EXERCISE.values())
    lines += ["", f"Expected total: {total_expected} tests.", ""]
    if problems:
        lines += ["Verification failed:", ""] + [f"- {p}" for p in problems]
    else:
        lines += [f"**All {total_expected} tests passed with no skips.**"]

    summary = "\n".join(lines) + "\n"
    print(summary)
    step_summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if step_summary:
        with open(step_summary, "a", encoding="utf-8") as output:
            output.write(summary)
    if problems:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
