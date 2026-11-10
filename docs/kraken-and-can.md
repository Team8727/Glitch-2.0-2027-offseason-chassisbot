# What Is a Kraken? (and the rest of the CAN hardware)

## Kraken X60 / X44

A **Kraken** is a brushless FRC motor made by CTR Electronics (CTRE). It is not a motor
controller by itself in the way a CIM + Talon was; the Kraken has an integrated Talon FX
controller on the back. That means:

- It talks on **CAN**, not PWM.
- It has its own **CAN ID**.
- It stores its own configuration (current limits, inversion, gear ratio, PID) in firmware.
- Phoenix Tuner X is the tool you use to see it, configure it, and run its self-test.

**X60** is the larger, higher-power Kraken used for drive. **X44** is the compact version used
for steering on this chassis. The prompt's "X66" does not exist in CTRE's catalog; the confirmed
drive motor is the X60.

## Talon FX / Falcon / NEO — the quick map

| Device | Controller | Notes |
|---|---|---|
| Kraken X60/X44 | Integrated Talon FX | CTRE, CAN, Phoenix 6 |
| Falcon 500 | Integrated Talon FX | Older CTRE motor |
| NEO / NEO 550 | External Spark MAX/Flex | REV, CAN |
| CIM | External Talon SRX / Victor | Legacy |

If someone says "the Kraken is timing out," they usually mean the **Talon FX on the Kraken** is
not responding on CAN: wrong ID, wrong bus, unpowered, or missing termination.

## CAN IDs

Every CAN device needs a unique integer ID on its bus. The roboRIO native CAN bus and a
CANivore are **separate buses**; the same ID can exist on both, and a device on one is invisible
from the other. That is exactly why the 2026 CANdle worked with `new CANdle(49)` but not when
the code targeted the CANivore.

## CANivore

A CANivore is a USB-to-CAN-FD adapter. It gives you a separate, faster CAN bus and offloads CAN
from the roboRIO. You name it in Tuner X. The code must use that exact name:

```java
new CANBus("Swerve Drive", "./logs/swerve.hoot")
```

If the name does not match, every device on that bus times out.

## Pigeon 2

A Pigeon 2 is a CAN gyro. It gives yaw/pitch/roll and is the source of heading for odometry.
It must be mounted rigidly and its mount pose must match the code. If the robot drives at an
angle after auto, suspect the Pigeon mount or the pose reset, not the path.

## CANdle

A CANdle is a CTRE LED controller. It can drive a long addressable strip and render animations
on-device. The important settings are:

- **LED count** — must match the physical strip, or the last LEDs stay dark.
- **Strip type** — RGB vs GRB vs RGBW; wrong type swaps colors.
- **Brightness** — high brightness draws current and heats the strip.
- **Bus** — native CAN or CANivore; the device is only visible on the bus it is wired to.

On the 2026 robot the strip had **88 LEDs**, not 91, and the CANdle was on **native CAN, ID 49**.

## PhotonVision / Rubik Pi

The Rubik Pi is a small computer that runs PhotonVision. Cameras plug into it over USB; it
publishes poses and targets over NetworkTables to the roboRIO. The roboRIO does not talk to the
camera directly. If vision is missing:

1. Is the Rubik Pi powered and booted?
2. Is it on the same network as the radio/roboRIO?
3. Does PhotonVision show the camera?
4. Are the camera names in the code the same as in PhotonVision?
5. Is the robot-to-camera transform measured, not guessed?

## The 60-second version

- Kraken = brushless motor + Talon FX controller, on CAN.
- CAN ID = house address; must be unique on that bus.
- CANivore = separate CAN bus over USB; the name must match.
- Pigeon 2 = gyro on CAN; mount pose matters.
- CANdle = LED controller on CAN; count and color order matter.
- Rubik Pi = vision computer; Ethernet to radio, USB to cameras.
- If a device "times out," check power, bus, ID, and termination in that order.
