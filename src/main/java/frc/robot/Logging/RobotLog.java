package frc.robot.Logging;

import java.util.HashMap;
import java.util.Map;

import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class RobotLog {
    private static final Map<String, LogEntry> entries = new HashMap<>();
    private static final double DEFAULT_MIN_PERIOD_SEC = 0.0;

    private RobotLog() {}

    public static void log(String key, String value) {
        putString(key, value);
    }

    public static void log(String key, double value) {
        putNumber(key, value);
    }

    public static void log(String key, boolean value) {
        putBoolean(key, value);
    }

    public static void putNumber(String key, double value) {
        putNumber(key, value, DEFAULT_MIN_PERIOD_SEC);
    }

    public static void putNumber(String key, double value, double minPeriodSec) {
        if (shouldLog(key, minPeriodSec)) {
            SmartDashboard.putNumber(key, value);
        }
    }

    public static void putString(String key, String value) {
        putString(key, value, DEFAULT_MIN_PERIOD_SEC);
    }

    public static void putString(String key, String value, double minPeriodSec) {
        if (shouldLog(key, minPeriodSec)) {
            SmartDashboard.putString(key, value);
        }
    }

    public static void putBoolean(String key, boolean value) {
        putBoolean(key, value, DEFAULT_MIN_PERIOD_SEC);
    }

    public static void putBoolean(String key, boolean value, double minPeriodSec) {
        if (shouldLog(key, minPeriodSec)) {
            SmartDashboard.putBoolean(key, value);
        }
    }

    private static boolean shouldLog(String key, double minPeriodSec) {
        if (minPeriodSec <= 0) return true;
        double now = Timer.getFPGATimestamp();
        LogEntry entry = entries.get(key);
        if (entry == null) {
            entries.put(key, new LogEntry(now));
            return true;
        }
        if (now - entry.lastLogTime >= minPeriodSec) {
            entry.lastLogTime = now;
            return true;
        }
        return false;
    }

    private static class LogEntry {
        double lastLogTime;
        LogEntry(double time) {
            this.lastLogTime = time;
        }
    }
}