package com.example.beinterviewprep.file.service;

final class FileNames {

  static final String FALLBACK_NAME = "file";
  private static final int MAX_LENGTH = 255;

  private FileNames() {}

  static String sanitize(String clientName) {
    if (clientName == null) {
      return FALLBACK_NAME;
    }
    String lastSegment = clientName.substring(lastSeparatorIndex(clientName) + 1);
    String printable = lastSegment.replaceAll("\\p{Cntrl}", "").strip();
    if (printable.isEmpty() || printable.equals(".") || printable.equals("..")) {
      return FALLBACK_NAME;
    }
    return printable.length() > MAX_LENGTH
        ? printable.substring(printable.length() - MAX_LENGTH)
        : printable;
  }

  private static int lastSeparatorIndex(String name) {
    return Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
  }
}
