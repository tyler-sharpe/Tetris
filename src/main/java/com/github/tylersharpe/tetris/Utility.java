package com.github.tylersharpe.tetris;

import java.awt.*;
import java.util.List;
import java.util.Random;

public final class Utility {

  private static final Random RANDOM = new Random();

  private Utility() {}

  public static String formatSeconds(long seconds) {
    long totalMinutes = seconds / 60;
    long secondsLeftover = seconds % 60;
    return (totalMinutes < 10 ? "0" : "") + totalMinutes +
           ":" +
           (secondsLeftover < 10 ? "0" : "") + secondsLeftover;
  }

  static <T> T sample(List<T> items) {
    return items.get(RANDOM.nextInt(items.size()));
  }

  static <T> T sample(T[] values) {
    return values[RANDOM.nextInt(values.length)];
  }

  public static Color getRandomColor() {
    return sample(BlockType.values()).getColor();
  }
}
