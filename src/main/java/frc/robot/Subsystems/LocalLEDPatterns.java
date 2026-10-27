package frc.robot.Subsystems;

import edu.wpi.first.wpilibj.AddressableLEDBuffer;
import edu.wpi.first.wpilibj.LEDPattern;
import edu.wpi.first.wpilibj.RobotController;
import edu.wpi.first.wpilibj.util.Color;

import static edu.wpi.first.units.Units.Microseconds;
import static edu.wpi.first.units.Units.Seconds;

public final class LocalLEDPatterns {

  private LocalLEDPatterns() {}

  public static LEDPattern fire(LEDPattern pattern, double updateTime, Color shiftColor, double speed) {
    return (reader, writer) -> {

      AddressableLEDBuffer tempBuffer = new AddressableLEDBuffer(reader.getLength());
      pattern.applyTo(tempBuffer);

      double randomOffset = 0.45 + (0.005 - Math.random() * 0.01);
      long actualUpdateTime = (long) Seconds.of(updateTime).in(Microseconds);
      long updateLimit = (long) Seconds.of(0.039).in(Microseconds);
      long robotTime = RobotController.getTime();
      double usefulTime = speed * Math.asin(Math.sin((double) RobotController.getTime() / 1000000));

      if (robotTime % actualUpdateTime < updateLimit && robotTime % actualUpdateTime > 0) {

        reader.forEach((index, red, green, blue) -> {

          int r;
          int g;
          int b;

          int realRed = reader.getRed(index);
          int realGreen = reader.getGreen(index);
          int realBlue = reader.getBlue(index);

          double fadeConstant = 10;

          if (realRed < tempBuffer.getRed(index) / Math.pow(fadeConstant, 2)) {
            realRed = 0;
            r = tempBuffer.getRed(index);
          } else {
            r = (tempBuffer.getRed(index) + (9 * realRed)) / 10;
          }
          if (realGreen < tempBuffer.getGreen(index) / Math.pow(fadeConstant, 2)) {
            realGreen = 0;
            g = tempBuffer.getGreen(index);
          } else {
            g = (tempBuffer.getGreen(index) + (9 * realGreen)) / 10;
          }
          if (realBlue < tempBuffer.getBlue(index) / Math.pow(fadeConstant, 2)) {
            realBlue = 0;
            b = tempBuffer.getBlue(index);
          } else {
            b = (tempBuffer.getBlue(index) + (9 * realBlue)) / 10;
          }

          if (r < 10) {
            r = 0;
          }
          if (g < 10) {
            g = 0;
          }
          if (b < 10) {
            b = 0;
          }

          if (shiftColor != null) {
            if (shiftColor == Color.kRed) {
              g = (int) Math.min(g, (0.5 * reader.getLength() * g) / (index + 1));
              b = (int) Math.min(b, (0.5 * reader.getLength() * b) / (index + 1));
            } else if (shiftColor == Color.kGreen) {
              r = (int) Math.min(r, (0.5 * reader.getLength() * r) / (index + 1));
              b = (int) Math.min(b, (0.5 * reader.getLength() * b) / (index + 1));
            } else if (shiftColor == Color.kBlue) {
              r = (int) Math.min(r, (0.5 * reader.getLength() * r) / (index + 1));
              g = (int) Math.min(g, (0.5 * reader.getLength() * g) / (index + 1));
            } else {
              r = (int) Math.min(r, (0.8 * reader.getLength() * r) / (index + 1));
              g = (int) Math.min(g, (0.8 * reader.getLength() * g) / (index + 1));
              b = (int) Math.min(b, (0.8 * reader.getLength() * b) / (index + 1));
            }
          }

          boolean isLit = false;
          boolean nearLow = false;
          boolean nearHigh = false;
          boolean midLow = false;
          boolean midHigh = false;
          boolean farLow = false;
          boolean farHigh = false;

          double flame = ((Math.sin(usefulTime)
              + Math.sin(usefulTime / randomOffset)
              + Math.pow(Math.sin(usefulTime), 2)
              + 3 * (Math.pow(Math.sin(usefulTime / randomOffset), 2))
              + (usefulTime / 6)
              + 1)
              * reader.getLength() / 8) + 0.1;

          double energy = Math.cos(usefulTime)
              + (Math.cos(usefulTime / randomOffset) * 1 / randomOffset)
              + (2 * Math.sin(usefulTime) * Math.cos(usefulTime))
              + (6 * Math.sin(usefulTime / randomOffset) * Math.cos(usefulTime / randomOffset) * 1 / randomOffset)
              + 1 / 6;

          if (r + g + b != 0) {
            isLit = true;
          }
          if (index < reader.getLength() - 1 && reader.getRed(index + 1) + reader.getGreen(index + 1) + reader.getBlue(index + 1) != 0) {
            nearHigh = true;
          }
          if (index < reader.getLength() - 2 && reader.getRed(index + 2) + reader.getGreen(index + 2) + reader.getBlue(index + 2) != 0) {
            midHigh = true;
          }
          if (index < reader.getLength() - 3 && reader.getRed(index + 3) + reader.getGreen(index + 3) + reader.getBlue(index + 3) != 0) {
            farHigh = true;
          }
          if (index > 2 && (reader.getRed(index - 1) + reader.getGreen(index - 1) + reader.getBlue(index - 1)) != 0) {
            nearLow = true;
          }
          if (index > 3 && (reader.getRed(index - 2) + reader.getGreen(index - 2) + reader.getBlue(index - 2)) != 0) {
            midLow = true;
          }
          if (index > 4 && (reader.getRed(index - 3) + reader.getGreen(index - 3) + reader.getBlue(index - 3)) != 0) {
            farLow = true;
          }

          if (index < flame) {
            if (energy > 0 && Math.random() < 0.8) {
              writer.setRGB(index, r, g, b);
            } else if (energy > 0) {
              writer.setRGB(index, (int) (r / fadeConstant), (int) (g / fadeConstant), (int) (b / fadeConstant));
            } else if (energy < 0 && Math.random() < 0.9) {
              writer.setRGB(index, r, g, b);
            } else {
              writer.setRGB(index, (int) (r / fadeConstant), (int) (g / fadeConstant), (int) (b / fadeConstant));
            }
          } else if (energy > 0 && Math.random() > 0.5
              && ((!isLit && (nearHigh || midHigh || farHigh))
              || (isLit && (nearLow && midLow && farLow)))) {
            writer.setRGB(index, r, g, b);
          } else if (energy > 0) {
            writer.setRGB(index, (int) (r / fadeConstant), (int) (g / fadeConstant), (int) (b / fadeConstant));
          } else if (energy < 0 && Math.random() * reader.getLength() * 2 < flame) {
            writer.setRGB(index, r, g, b);
          } else {
            writer.setRGB(index, (int) (r / fadeConstant), (int) (g / fadeConstant), (int) (b / fadeConstant));
          }
        });
      }
    };
  }
}