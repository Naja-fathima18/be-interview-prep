package com.example.beinterviewprep.file.domain;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public enum FileType {
  JPEG("image/jpeg", Set.of("jpg", "jpeg"), new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
  PNG(
      "image/png",
      Set.of("png"),
      new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}),
  PDF("application/pdf", Set.of("pdf"), new byte[] {0x25, 0x50, 0x44, 0x46, 0x2D});

  public static final int SIGNATURE_LENGTH = 8;

  private final String mediaType;
  private final Set<String> extensions;
  private final byte[] signature;

  FileType(String mediaType, Set<String> extensions, byte[] signature) {
    this.mediaType = mediaType;
    this.extensions = extensions;
    this.signature = signature;
  }

  public String mediaType() {
    return mediaType;
  }

  public static Optional<FileType> detect(byte[] header) {
    return Arrays.stream(values()).filter(type -> type.matches(header)).findFirst();
  }

  public boolean acceptsExtensionOf(String fileName) {
    int dot = fileName.lastIndexOf('.');
    if (dot < 0 || dot == fileName.length() - 1) {
      return true;
    }
    return extensions.contains(fileName.substring(dot + 1).toLowerCase(Locale.ROOT));
  }

  private boolean matches(byte[] header) {
    if (header.length < signature.length) {
      return false;
    }
    return Arrays.equals(header, 0, signature.length, signature, 0, signature.length);
  }
}
