package frc.robot.LEDs;

import static frc.robot.Constants.*;

public class BlueFlameAnimation {
    private final int startIdx;
    private final int endIdx;
    private final int numLeds;
    private double brightness = LED_BRIGHTNESS;
    private double sparking = FIRE_SPARKING;
    private double cooling = FIRE_COOLING;
    private double frameRate = FIRE_FRAME_RATE;

    private byte[] heat;
    private int[] lastColors;
    private double lastFrameTime = 0.0;
    private boolean redAlliance = true;

    public BlueFlameAnimation(int startIdx, int endIdx) {
        this.startIdx = startIdx;
        this.endIdx = endIdx;
        this.numLeds = endIdx - startIdx + 1;
        this.heat = new byte[numLeds];
        this.lastColors = new int[numLeds];
    }

    public void setAlliance(boolean red) {
        redAlliance = red;
    }

    public BlueFlameAnimation withBrightness(double brightness) {
        this.brightness = brightness;
        return this;
    }

    public BlueFlameAnimation withSparking(double sparking) {
        this.sparking = sparking;
        return this;
    }

    public BlueFlameAnimation withCooling(double cooling) {
        this.cooling = cooling;
        return this;
    }

    public BlueFlameAnimation withFrameRate(double frameRate) {
        this.frameRate = frameRate;
        return this;
    }

    public int[] getLEDs(double timestamp) {
        double frameTime = 1.0 / frameRate;
        if (timestamp - lastFrameTime < frameTime) {
            return lastColors;
        }
        lastFrameTime = timestamp;

        // Cool down every cell
        for (int i = 0; i < numLeds; i++) {
            int cool = (int)((heat[i] & 0xFF) * cooling);
            heat[i] = (byte)Math.max(0, (heat[i] & 0xFF) - cool);
        }

        // Heat from each cell drifts upward
        for (int i = numLeds - 1; i >= 2; i--) {
            int t = (heat[i] & 0xFF) + (heat[i - 1] & 0xFF) + (heat[i - 2] & 0xFF);
            heat[i] = (byte)(t / 3);
        }

        // Random ignition (sparking)
        for (int i = 0; i < (int)(sparking * 2); i++) {
            int idx = (int)(Math.random() * numLeds);
            int newHeat = (int)((heat[idx] & 0xFF) + (Math.random() * 128 + 64));
            heat[idx] = (byte)Math.min(255, newHeat);
        }

        // Convert heat to color
        for (int i = 0; i < numLeds; i++) {
            int t = heat[i] & 0xFF;
            int r, g, b;

            if (redAlliance) {
                // Red/orange flame
                r = Math.min(255, t * 2);
                g = Math.min(255, t * 2 - 128);
                b = Math.min(255, Math.max(0, t * 2 - 196));
            } else {
                // Blue/cyan flame (Lynk-style)
                r = Math.min(255, Math.max(0, t * 2 - 196));
                g = Math.min(255, t * 2 - 128);
                b = Math.min(255, t * 2);
            }

            int color = ((int)(r * brightness) & 0xFF) << 16
                      | ((int)(g * brightness) & 0xFF) << 8
                      | ((int)(b * brightness) & 0xFF);

            lastColors[i] = color;
        }

        return lastColors;
    }

    public void reset() {
        heat = new byte[numLeds];
    }
}