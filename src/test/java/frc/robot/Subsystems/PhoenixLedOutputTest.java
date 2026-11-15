package frc.robot.Subsystems;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PhoenixLedOutputTest {
  @Test
  @DisplayName("External pixel zero maps to device index 8, preserving the RGB channels")
  void firstExternalPixelSkipsOnboardLeds() {
    var request = PhoenixLedOutput.request(0, 1, 12, 34, 56);
    assertEquals(8, request.LEDStartIndex, "Indices 0–7 belong to CANdle's onboard LEDs");
    assertEquals(8, request.LEDEndIndex, "A single pixel has the same start and inclusive end");
    assertEquals(12, request.Color.Red);
    assertEquals(34, request.Color.Green);
    assertEquals(56, request.Color.Blue);
  }

  @Test
  @DisplayName("Blanking 60 external pixels covers exactly device indices 8 through 67")
  void wholeStripRangeHasInclusiveEnd() {
    var request = PhoenixLedOutput.request(0, 60, 0, 0, 0);
    assertEquals(8, request.LEDStartIndex);
    assertEquals(67, request.LEDEndIndex, "An off-by-one would leave the last pixel lit");
  }

  @Test
  @DisplayName("Out-of-range requests fail explicitly instead of addressing unrelated LEDs")
  void rejectsInvalidRanges() {
    assertThrows(IllegalArgumentException.class, () -> PhoenixLedOutput.request(-1, 1, 0, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> PhoenixLedOutput.request(0, 0, 0, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> PhoenixLedOutput.request(391, 2, 0, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> PhoenixLedOutput.request(1, Integer.MAX_VALUE, 0, 0, 0));
    assertEquals(399, PhoenixLedOutput.request(391, 1, 0, 0, 0).LEDEndIndex);
  }
}
