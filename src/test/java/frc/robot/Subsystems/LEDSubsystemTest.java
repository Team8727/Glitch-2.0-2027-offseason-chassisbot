package frc.robot.Subsystems;

import static org.junit.jupiter.api.Assertions.*;

import edu.wpi.first.hal.HAL;
import edu.wpi.first.wpilibj.LEDPattern;
import edu.wpi.first.wpilibj.util.Color;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LEDSubsystemTest {
  private final AtomicLong clock = new AtomicLong();
  private final List<String> errors = new ArrayList<>();
  private final FakeOutput output = new FakeOutput();
  private LEDSubsystem leds;

  @BeforeAll
  static void initializeHal() {
    assertTrue(HAL.initialize(500, 0), "Desktop HAL must initialize before subsystem tests");
  }

  @BeforeEach
  void createSubsystem() {
    leds = new LEDSubsystem(output, clock::get, errors::add);
  }

  @AfterEach
  void unregisterSubsystem() {
    CommandScheduler.getInstance().unregisterSubsystem(leds);
  }

  @Test
  @DisplayName("Mode transitions select green when disabled and blue in autonomous/teleop")
  void modesChooseTheirDocumentedBase() {
    leds.start();
    assertSame(leds.greenFire, leds.strip.getBasePattern(), "Disabled mode should use green fire");
    assertSame(leds.greenFire, leds.strip.getCurrentPattern());
    leds.autoInit();
    assertSame(leds.blueFire, leds.strip.getCurrentPattern(), "Autonomous should use blue fire");
    leds.start();
    leds.teleopInit();
    assertSame(leds.blueFire, leds.strip.getBasePattern(), "Teleop should use blue fire");
    assertSame(leds.blueFire, leds.strip.getCurrentPattern());
  }

  @Test
  @DisplayName("An override stays active before expiry and restores the base at the exact boundary")
  void timedOverrideRestoresBaseAtBoundary() {
    leds.strip.setBase(LEDPattern.solid(Color.kBlue));
    leds.setOverride(LEDPattern.solid(Color.kRed), 10.0);
    leds.strip.update(9.5);
    assertEquals(Color.kRed, leds.strip.getBufferView().getLED(0),
        "The override must remain visible before its duration expires");
    leds.strip.update(0.5);
    assertEquals(Color.kBlue, leds.strip.getBufferView().getLED(0),
        "At ten seconds the rendered LED must return to the blue base");
  }

  @Test
  @DisplayName("A clock starting at zero still advances override time on the next periodic call")
  void zeroClockIsNotConfusedWithUninitializedTime() {
    leds.strip.setBase(LEDPattern.solid(Color.kBlue));
    leds.setOverride(LEDPattern.solid(Color.kRed), 0.5);
    leds.periodic();
    clock.set(1_000_000);
    leds.periodic();
    assertEquals(Color.kBlue, leds.strip.getBufferView().getLED(0),
        "One elapsed second must expire the half-second override, even if the clock began at zero");
  }

  @Test
  @DisplayName("An indefinite override persists until explicitly cleared")
  void indefiniteOverrideCanBeCleared() {
    leds.strip.setBase(LEDPattern.solid(Color.kBlue));
    leds.setOverride(LEDPattern.solid(Color.kRed));
    leds.strip.update(1_000);
    assertEquals(Color.kRed, leds.strip.getBufferView().getLED(0));
    leds.clearOverride();
    leds.strip.update(0);
    assertEquals(Color.kBlue, leds.strip.getBufferView().getLED(0),
        "Clearing an override must restore the current base immediately on the next render");
  }

  @Test
  @DisplayName("A mode change cancels the old timed override")
  void modeChangeCancelsOverride() {
    leds.start();
    leds.setOverride(LEDPattern.solid(Color.kRed), 10);
    leds.autoInit();
    assertSame(leds.blueFire, leds.strip.getCurrentPattern(),
        "Entering autonomous must replace the previous override");
    leds.strip.update(11);
    assertSame(leds.blueFire, leds.strip.getCurrentPattern(),
        "The old override must not return later");
  }

  @Test
  @DisplayName("Test mode selects a ten-second scan, then restores the current base")
  void testScanExpiresAfterTenSeconds() {
    leds.teleopInit();
    leds.testInit();
    assertSame(leds.whiteScan, leds.strip.getCurrentPattern());
    leds.strip.update(10);
    assertSame(leds.blueFire, leds.strip.getCurrentPattern(),
        "The bench scan must not permanently replace the mode indication");
  }

  @Test
  @DisplayName("Null patterns and null bases produce off pixels instead of a crash")
  void nullPatternsFallBackToOff() {
    leds.setOverride(null);
    leds.strip.update(0);
    assertEquals(Color.kBlack, leds.strip.getBufferView().getLED(0));
    leds.strip.setBase(null);
    leds.setOverride(LEDPattern.solid(Color.kRed), 1);
    leds.strip.update(1);
    assertEquals(Color.kBlack, leds.strip.getBufferView().getLED(0));
  }

  @Test
  @DisplayName("Rendering reaches the output for all 60 pixels, at most once per 200 ms")
  void writesRespectRefreshBoundary() {
    leds.setOverride(LEDPattern.solid(Color.kRed));
    clock.set(199_999);
    leds.periodic();
    assertTrue(output.writes.isEmpty(), "No hardware writes are due before 200 ms");
    clock.set(200_000);
    leds.periodic();
    assertEquals(60, output.writes.size(), "A frame must cover all 60 external strip pixels");
    assertArrayEquals(new int[] {0, 1, 255, 0, 0}, output.writes.get(0));
    assertArrayEquals(new int[] {59, 1, 255, 0, 0}, output.writes.get(59));
    clock.set(399_999);
    leds.periodic();
    assertEquals(60, output.writes.size(), "Extra periodic calls must not increase CAN write rate");
    clock.set(400_000);
    leds.periodic();
    assertEquals(120, output.writes.size());
  }

  @Test
  @DisplayName("Disabling blanks the strip once, suppresses writes, and allows re-enable")
  void disabledOutputStaysBlankUntilReenabled() {
    leds.setOverride(LEDPattern.solid(Color.kRed));
    leds.setEnabled(false);
    leds.setEnabled(false);
    assertEquals(1, output.writes.size(), "Repeated disable calls should not flood the bus");
    assertArrayEquals(new int[] {0, 60, 0, 0, 0}, output.writes.get(0));
    clock.set(1_000_000);
    leds.periodic();
    assertEquals(1, output.writes.size(), "Disabled rendering must not reach the device");
    leds.setEnabled(true);
    leds.periodic();
    assertEquals(61, output.writes.size(), "Re-enabling should resume normal frame writes");
  }

  @Test
  @DisplayName("An override timer continues while output is disabled")
  void timersContinueWhileOutputIsDisabled() {
    leds.strip.setBase(LEDPattern.solid(Color.kBlue));
    leds.setOverride(LEDPattern.solid(Color.kRed), 1);
    leds.setEnabled(false);
    leds.periodic();
    clock.set(2_000_000);
    leds.periodic();
    leds.setEnabled(true);
    leds.periodic();
    assertArrayEquals(new int[] {0, 1, 0, 0, 255}, output.writes.get(1),
        "Re-enabling should show the base, not an expired override");
  }

  @Test
  @DisplayName("A failed device retries at the normal interval and logs once per fault episode")
  void failuresAreThrottledAndRecoveryResetsDiagnostics() {
    output.healthy = false;
    clock.set(200_000);
    leds.periodic();
    assertEquals(1, output.writes.size(), "Stop the frame after the first failed write");
    assertEquals(1, errors.size(), "The first failed frame needs a diagnostic");
    clock.set(220_000);
    leds.periodic();
    assertEquals(1, output.writes.size(), "A fault must not trigger retries every robot loop");
    clock.set(400_000);
    leds.periodic();
    assertEquals(2, output.writes.size());
    assertEquals(1, errors.size(), "A continuing fault should not flood the driver station");
    output.healthy = true;
    clock.set(600_000);
    leds.periodic();
    assertEquals(62, output.writes.size(), "Recovery should complete a whole frame");
    output.healthy = false;
    clock.set(800_000);
    leds.periodic();
    assertEquals(2, errors.size(), "A new fault after recovery deserves a new diagnostic");
  }

  private static final class FakeOutput implements LedOutput {
    final List<int[]> writes = new ArrayList<>();
    boolean healthy = true;

    @Override
    public boolean setLEDs(int start, int count, int red, int green, int blue) {
      writes.add(new int[] {start, count, red, green, blue});
      return healthy;
    }
  }
}
