# LED Subsystem — CTRE CANdle, Glitch Team Fire Patterns

## Goal

Add a CANdle-backed LED subsystem to the offseason chassisbot that renders **fire
patterns in the team's green/blue colors**. Source-only scope: the consuming WPILib
2026 Java project already exists (or is scaffolded separately); this plan produces
the subsystem Java files and documents the integration points.

## Decisions (confirmed with user)

| Topic | Decision |
|---|---|
| Hardware | CTRE **CANdle**, CAN **ID 14**, roboRIO construction CAN bus |
| Strip | ~60 pixels, **single continuous section** |
| Team palette | Deep emerald `new Color(0, 60, 35)` + royal blue `new Color(5, 20, 90)`; bright green/blue flame tips. One tunable constants block |
| Mode behavior | Disabled = **green fire**; teleop & auto = **blue fire**; **override API** so other code (drive/AI) can push temporary patterns that auto-revert to the base pattern |
| Pattern engine | Reuse GlitchLib's `Glitch.Lib.LEDs.GlitchLEDPatterns.fire(...)` compositor — do **not** modify GlitchLib |
| Code placement | New classes in this repo under `frc.robot.Subsystems` |
| Language / WPILib | Java, WPILib **2026** (Phoenix 6 vendordep, e.g. `Phoenix6-26.1.1.json` as in the 2026 repo) |
| Eye safety | Pattern push throttled to >= 0.2 s (matching GlitchLib's `AbstractLEDS` cadence); configurable brightness scalar |

## Why not the existing GlitchLib `AbstractLEDS`

GlitchLib's `AbstractLEDS` drives a PWM `AddressableLED`, not a CANdle. Phoenix 6's
`com.ctre.phoenix6.led.Candle` cannot consume an `AddressableLEDBuffer`; pixels must
be pushed per-pixel via `candle.setLEDs(startIndex, count, r, g, b)`. The WPILib
`LEDPattern` stack (including GlitchLib's `fire(...)`) is hardware-agnostic, so we:
apply patterns to an on-RIO `AddressableLEDBuffer` (via a buffer view per section),
then stream the per-pixel RGB values to the CANdle. This keeps the whole pattern
library (fire, purple, ripple, etc.) working unchanged.

## Files to create (source only)

### 1. `src/main/java/frc/robot/Subsystems/CandleLEDs.java` — abstract CANdle driver base

Mirrors GlitchLib `AbstractLEDS` semantics but replaces the PWM strip with a CANdle.
Do not copy GlitchLib code; write a slim equivalent.

- `extends SubsystemBase`
- Fields: `Candle candle`, `AddressableLEDBuffer stripBuffer`, one `Section` (single
  strip; structure the constructor to accept `int ledCount` so future section splits
  are a parameter change, not a rewrite).
- Constructor takes `(int candleId, int ledCount)`; builds `new Candle(candleId)`
  and configures via `candle.getConfigurator().apply(config)`:
  - `ConfigRegisters`/`CandleConfiguration`: `brightnessScalar` (default ~0.7),
    `v5Enabled = true` (CANdle VBUS powers the strip — mandatory), `stripType` set
    per physical strip ordering (start `kRGB`; tune on bench if colors are swapped).
  - Verify exact field names against the Phoenix 6 version the consuming project
    vendors (26.1.1); rename fields if the vendored API differs.
- Inner `Section` class with the same contract as GlitchLib's:
  - `setPattern(LEDPattern)` (infinite), `setPattern(LEDPattern, double durationSeconds)`
    (auto-reverts to base), `setBase(LEDPattern)`, `getBufferView()`,
    `getCurrentPattern()`, `getBasePattern()`.
- `periodic()`:
  - Track `deltaTime` from `RobotController.getTime()` (like GlitchLib).
  - `section.update(deltaTime)` → applies active/base pattern to the buffer view.
  - Push to CANdle at most every `0.2 s`: for each pixel
    `candle.setLEDs(index, 1, r, g, b)` from the buffer. Mismatched/shouldWrite
    overloads: use the basic 5-arg overload; check returned `ConfigError`.
  - A single `ConfigError != OK` or unhealthy status signal → log once (`DriverStation.reportError`
    or `SmartDashboard`), stop pushing for that tick; do not spam.
- `setEnabled(boolean)`/`isRunning()` helpers so a future emergency-off hook can blank
  the strip without scheduler churn (e.g. `candle.setLEDs(0, 60, 0, 0, 0)`).

### 2. `src/main/java/frc/robot/Subsystems/LEDSubsystem.java` — team subsystem

- `package frc.robot.Subsystems;` (team convention: capitalized `Subsystems`).
- `extends CandleLEDs`, singleton via `static getInstance()` (team convention).
- Tunable constants block (single place to adjust after seeing it live):
  - `CANDLE_ID = 14`
  - `LED_COUNT = 60`
  - `EMERALD = new Color(0, 60, 35)`, `ROYAL_BLUE = new Color(5, 20, 90)`
  - `FIRE_SPEED = 0.5`, `FIRE_UPDATE_TIME = 0.11` (GlitchLib `fire` defaults)
  - `BRIGHTNESS_SCALAR`, `LED_PUSH_ACCEPTABLE_PERIOD_SECONDS = 0.2`
- Fire patterns (built from `Glitch.Lib.LEDs.GlitchLEDPatterns.fire(...)`):
  - `greenFire = fire(LEDPattern.solid(EMERALD), Color.kGreen)` — emerald body,
    bright green flame tips.
  - `blueFire = fire(LEDPattern.solid(ROYAL_BLUE), Color.kBlue)` — royal blue body,
    bright blue flame tips.
  - Expose as `public final LEDPattern` fields (like the 2026 `LEDSubsystem` exposes
    `leftSide`, `pip`, `rightSide`).
- Mode methods (same lifetime naming as the 2026 robot):
  - `start()` → `setBase(greenFire)` (disabled idle).
  - `autoInit()` → `setBase(blueFire)`.
  - `teleopInit()` → `setBase(blueFire)`.
  - `testInit()` → temporary scrolling white scan (`LEDPattern.scrollAtRelativeSpeed`)
    for verifying pixel order / pixel 0 on the bench.
- Override API (delegates to `Section.setPattern` so timed overrides auto-revert):
  - `setOverride(LEDPattern p)`
  - `setOverride(LEDPattern p, double seconds)`
  - `clearOverride()`
- JavaDoc comments on public API per team AGENTS.md ("well-documented through
  commenting") — GlitchLib/2026 codebase style.

## Validation

- **Compile**: in the consuming project (WPILib 2026, Phoenix 6 vendordep +
  GlitchLib submodule on the main source path): `./gradlew build`.
- **Unit test (optional, recommended)**: JUnit + sim HAL test for `Section`
  semantics (timed pattern reverts to base after duration; override resets), modeled
  on GlitchLib's `Glitch/Lib/test/Glitch/Lib/LEDTests.java`. Requires the consuming
  project's test source path to include `GlitchLib/Glitch/Lib/test`
  (mirrors the 2026 `build.gradle` sourceSets).
- **Bench (hardware)**:
  - Phoenix Tuner X: CANdle enumerates at ID 14; strip powers on (`v5Enabled`).
  - `testInit()` white scan: confirm pixel 0 is the pixel physically nearest the
    CANdle connector; flip orientation via a `REVERSE` constant or
    `LEDPattern.reversed()` if backwards.
  - Confirm green fire in disabled, blue fire once enabled.
  - Lower brightness scalar if the strip is too intense in the shop.

## Integration notes for the consuming Robot code (not authored here)

- Construct once: `leds = LEDSubsystem.getInstance();`
- Wire lifetime hooks (as the 2026 `Robot.java` does for its `LEDSubsystem`):
  - `disabledInit()` → `leds.start();`
  - `autonomousInit()` → `leds.autoInit();`
  - `teleopInit()` → `leds.teleopInit();`
  - `testInit()` → `leds.testInit();`
- Scheduler calls `periodic()` automatically since it extends `SubsystemBase`.

## Prerequisites (existing project setup, not part of this plan)

- WPILib 2026 Gradle project (Java 17).
- `vendordeps/Phoenix6-26.1.1.json` (or newer 26.x) for `com.ctre.phoenix6.led.*`.
- GlitchLib git submodule at `GlitchLib/` with `GlitchLib/Glitch/Lib/main` added to
  the main source dirs (2026 `build.gradle` does exactly this).

## Risks / notes

- **Phoenix 6 API drift**: CANdle config field names/`setLEDs` overloads vary between
  Phoenix 6 releases. Implementer should compile against the vendored version and
  flag renames in a follow-up note; do not guess from memory.
- **Do NOT pull in Phoenix 5** for the CANdle — the vendordep must be Phoenix 6.
- **CAN load**: throttled push is ~60 pixels × 5 Hz ≈ 300 writes/s in short bursts —
  trivial on roboRIO construction CAN, but keep the 0.2 s throttle (also satisfies
  the team's anti-flicker/epilepsy rule).
- **Power**: 60 pixels on CANdle 5 V VBUS is well within budget; keep strip off while
  disabled-dead (brownout behavior is upstream's concern, not the subsystem's).
- **Fire cadence**: `GlitchLEDPatterns.fire` updates at its own ~39 ms window but the
  0.2 s push throttle governs on-strip smoothness — same behavior as the 2026 robot.

## Out of scope

- Project scaffolding (build files, vendordeps JSON, Gradle wrapper, `Main`/`Robot`).
- Changes to GlitchLib.
- Driver/operator bindings and commands that cycle patterns.
- Drive-speed reactive fire, alliance-colored modes, endgame effects.
- Phoenix 6 native hardware animations (CPU-side rendering only).