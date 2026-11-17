# Wiring Map — 2027 Chassis

## Power

```text
Battery
  -> main breaker
  -> PDH/PDP
     -> 4x drive Talon FX (X60)   [40 A breakers unless the electrical lead says otherwise]
     -> 4x steer Talon FX (X44)   [20-30 A breakers]
     -> roboRIO / SystemCore
     -> radio (poE or 12 V barrel, per radio)
     -> Rubik Pi (5 V/12 V buck, per its carrier board)
     -> CANdle (5 V rail enabled by config)
```

Verify every breaker and wire gauge with the electrical lead. This file is a map, not an approval.

## CAN

```text
CANivore (USB to roboRIO)
  -> 4x MK5n drive + steer Talon FX
  -> 4x CANcoder
  -> Pigeon 2 (ID 2)
  -> 120 ohm termination at both ends

roboRIO native CAN
  -> CANdle (ID 49)

roboRIO USB
  -> CANivore
```

The CANdle is on native CAN because that is the configuration that worked on the 2026 robot.
If the 2027 chassis moves it to the CANivore, change `StatusLEDs` to
`new CANdle(CANDLE_ID, TunerConstants.kCANBus)` and re-test.

## Ethernet / USB

```text
FrontL camera --USB--> Rubik Pi 3
FrontR camera --USB--> Rubik Pi 3
Rubik Pi 3 --Ethernet--> radio
radio --Ethernet--> roboRIO
Driver Station laptop --Wi-Fi/Ethernet--> radio
```

- PhotonVision runs on the Rubik Pi.
- Set the camera names to `FrontL` and `FrontR` so `VisionConstants` matches.
- Confirm the radio and Rubik Pi are on the same subnet as the roboRIO.

## roboRIO / SystemCore

| Port | Device |
|---|---|
| USB | CANivore |
| Ethernet | radio |
| CAN | CANdle (native) |
| PWM/DIO | none on this chassis |

SystemCore has native CAN ports. On `Beako-Syscore`, confirm whether the CANivore stays in
the chain before changing the bus name.

## Pre-power checklist

- [ ] Battery voltage > 12.0 V.
- [ ] Main breaker off, robot on blocks.
- [ ] CAN chain terminated at both ends.
- [ ] No loose ferrules or exposed conductor.
- [ ] CAN IDs match `robot-config.md`.
- [ ] Rubik Pi boots and PhotonVision UI is reachable.
- [ ] Driver Station sees the robot and the correct joystick.
