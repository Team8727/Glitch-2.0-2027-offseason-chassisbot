# Elastic + NetworkTables Notes

## Why the 2026 dashboard had NT problems

The 2026 robot published the same data through five different paths: `NetworkTableLogger`,
`SmartDashboard`, DogLog's NT mirror, URCL, and CTRE SignalLogger. That produces:

- duplicate topics with different names for the same value
- stale widgets pointing at topics that no longer exist
- a dashboard that connects but shows dashes
- high NT traffic during matches

The 2027 branch fixes this by publishing **one explicit table**:

```text
/Elastic/Drive/PoseX
/Elastic/Drive/PoseY
/Elastic/Drive/HeadingDeg
/Elastic/Drive/SpeedMps
/Elastic/Drive/OmegaDegPerSec
/Elastic/Drive/OdometryHz
/Elastic/Robot/BatteryVolts
/Elastic/Robot/LoopTimeMs
/Elastic/Vision/Active
/Elastic/Vision/AcceptedMeasurements
/Elastic/Vision/FrontLeftConnected
/Elastic/Vision/FrontRightConnected
/Elastic/LED/State
```

The only `SmartDashboard` topics are:

```text
/SmartDashboard/Field          (Field2d)
/SmartDashboard/Auto choices   (SendableChooser)
```

## Connection checklist

1. **One NT server.** The roboRIO is the server. Do not start a second `NetworkTableInstance`
   server on the driver laptop.
2. **Same network.** Laptop on the robot radio; robot and radio on the same subnet.
3. **Team number.** Elastic should connect to `roboRIO-8727-FRC.local` or the radio's IP.
4. **NT4.** Use NT4; do not mix in an old NT3 dashboard.
5. **Topic names.** Open Elastic's topic picker and confirm `/Elastic/...` exists. If the table is
   missing, the robot code did not publish it.
6. **No wildcard widgets.** Point every widget at an exact topic.
7. **Period.** 30-50 ms for driver values, 100 ms for graphs. Faster does not help and adds load.
8. **Field widget.** Use `/SmartDashboard/Field`; verify the field image is `Rebuilt` for this
   offseason chassis.

## Layout rules that keep it stable

- 3 tabs: `Driver`, `Diagnostics`, `Autos`.
- Driver tab: field, speed/heading, battery, auto chooser, match time, FMS info, scheduler.
- Diagnostics tab: loop time, odometry Hz, vision health, LED state, graphs.
- Autos tab: chooser, field, scheduler, match time.
- Do not put a widget on two tabs pointing at the same topic unless you intend to.
- Keep the JSON valid; CI JSON-lints `src/main/deploy`.

## If a widget shows a dash

1. Check the topic exists in the topic picker.
2. Check the robot is enabled (some values only publish while enabled).
3. Check the type: a `Text Display` will not render a boolean; use a `Boolean Box`.
4. Check the field path: `/Elastic/...` is case-sensitive.
5. Check the code actually calls the publisher every loop; `NetworkTables` only updates when
   `set()` is called.

## 2027 / SystemCore

WPILib 2027 removes `SmartDashboard` and `SendableChooser` from the old API. Use
`NetworkTableInstance` publishers and `Selectable`. Elastic must be a 2027-compatible build.
The `/Elastic/...` table design carries over unchanged; only the publishing API changes.
