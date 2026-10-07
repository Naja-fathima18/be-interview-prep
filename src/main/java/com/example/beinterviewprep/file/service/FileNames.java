package com.example.beinterviewprep.file.service;

import java.util.regex.Pattern;

final class FileNames {

  static final String FALLBACK_NAME = "file";
  static final int MAX_CODE_POINTS = 255;
  private static final Pattern UNSAFE_CHARACTERS =
      Pattern.compile("[\\p{Cntrl}\\u2028\\u2029\\u202A-\\u202E\\u2066-\\u2069]");

  private FileNames() {}

  static String sanitize(String clientName) {
    if (clientName == null) {
      return FALLBACK_NAME;
    }
    String lastSegment = clientName.substring(lastSeparatorIndex(clientName) + 1);
    String safe = UNSAFE_CHARACTERS.matcher(lastSegment).replaceAll("").strip();
    if (safe.isEmpty() || safe.equals(".") || safe.equals("..")) {
      return FALLBACK_NAME;
    }
    return keepLastCodePoints(safe);
  }

  private static String keepLastCodePoints(String name) {
    int excess = name.codePointCount(0, name.length()) - MAX_CODE_POINTS;
    return excess > 0 ? name.substring(name.offsetByCodePoints(0, excess)) : name;
  }

  private static int lastSeparatorIndex(String name) {
    return Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
  }
}
