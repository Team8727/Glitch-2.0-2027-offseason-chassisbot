"""Turn Gradle's JUnit XML into factual, reproducible feedback (no AI diagnosis)."""

import html
import os
from pathlib import Path
import xml.etree.ElementTree as ET


def summarize(report_dir, outcome):
    suites = [ET.parse(path).getroot() for path in sorted(report_dir.glob("TEST-*.xml"))]
    tests = [test for suite in suites for test in suite.findall("testcase")]
    skipped = sum(test.find("skipped") is not None for test in tests)
    failures = [test for test in tests if test.find("failure") is not None or test.find("error") is not None]
    robot = sum(test.get("classname", "").startswith("frc.robot.") and test.find("skipped") is None for test in tests)
    passed = len(tests) - skipped - len(failures)
    lines = ["## Build and behavioral tests", "",
             f"Build result: **{html.escape(outcome or 'not run')}**.", "",
             f"Tests: **{passed} passed**, **{len(failures)} failed**, **{skipped} skipped**. "
             f"Robot tests executed: **{robot}** (separate from shared-library tests).", ""]
    if not tests:
        lines += ["**No test results were produced. Robot behavior has not been verified.**",
                  "Check the first failed setup or build step; dependencies or compilation may have prevented tests from starting.", ""]
    elif robot == 0:
        lines += ["**No robot tests executed.** Library tests alone do not establish that this robot behaves correctly.", ""]
    if failures:
        lines += ["### Behaviors to investigate", ""]
        for test in failures[:20]:
            failure = test.find("failure")
            if failure is None:
                failure = test.find("error")
            classname = test.get("classname", "unknown")
            name = test.get("name", "unknown")
            message = failure.get("message") or failure.text or "See the detailed test report."
            # Escape report text so assertion messages cannot become HTML in the summary.
            lines += [f"**{html.escape(classname)} — {html.escape(name)}**", "",
                      "<pre>" + html.escape(message[:2000]) + "</pre>", "",
                      "Reproduce this class locally:", "",
                      "<pre>" + html.escape(f"./gradlew test --tests '{classname}'") + "</pre>", ""]
        if len(failures) > 20:
            lines += ["Additional failures are available in the detailed report.", ""]
        lines += ["Compare the expected and actual results with the intended behavior. "
                  "Do not change an expectation solely to make the test pass.", ""]
    elif tests and outcome != "success":
        lines += ["Tests are only part of verification. Inspect the failed build step before treating this revision as ready.", ""]
    lines += ["### What this evidence means", "",
              "The suite is designed to check compilation, LED state/timing, rendered output, CAN request mapping, "
              "and controlled fault handling. They do not prove wiring, physical light output, "
              "photosensitivity safety, real CAN utilization, or match performance.", "",
              "Run the full check with `./gradlew build`. Open `build/reports/tests/test/index.html` "
              "locally, or download **verification-reports** from this run. See `docs/testing.md` "
              "for the test contracts and hardware follow-up.", ""]
    return "\n".join(lines)


if __name__ == "__main__":
    summary = summarize(Path("build/test-results/test"), os.environ.get("BUILD_OUTCOME", "local run"))
    destination = os.environ.get("GITHUB_STEP_SUMMARY")
    if destination:
        with open(destination, "a", encoding="utf-8") as output:
            output.write(summary)
    else:
        print(summary)
