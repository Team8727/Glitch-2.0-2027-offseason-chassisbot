package frc.robot.Subsystems;

/** Writes a contiguous range of external-strip pixels; false means the write failed. */
@FunctionalInterface
interface LedOutput {
  boolean setLEDs(int start, int count, int red, int green, int blue);
}
