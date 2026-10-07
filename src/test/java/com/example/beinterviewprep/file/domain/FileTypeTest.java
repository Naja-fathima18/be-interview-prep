package com.example.beinterviewprep.file.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FileTypeTest {

  @Test
  void detectsJpegPngAndPdfFromLeadingBytes() {
    assertThat(FileType.detect(bytes(0xFF, 0xD8, 0xFF, 0xE0, 0, 0, 0, 0))).contains(FileType.JPEG);
    assertThat(FileType.detect(bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)))
        .contains(FileType.PNG);
    assertThat(FileType.detect("%PDF-1.7".getBytes())).contains(FileType.PDF);
  }

  @Test
  void detectsNothingForExecutableOrTruncatedContent() {
    assertThat(FileType.detect(bytes(0x4D, 0x5A, 0x90, 0, 3, 0, 0, 0))).isEmpty();
    assertThat(FileType.detect(bytes(0x89, 0x50, 0x4E))).isEmpty();
    assertThat(FileType.detect(new byte[0])).isEmpty();
  }

  @Test
  void acceptsMatchingOrMissingExtensionCaseInsensitively() {
    assertThat(FileType.JPEG.acceptsExtensionOf("photo.JPG")).isTrue();
    assertThat(FileType.JPEG.acceptsExtensionOf("photo.jpeg")).isTrue();
    assertThat(FileType.PDF.acceptsExtensionOf("report")).isTrue();
  }

  @Test
  void rejectsExtensionThatDisagreesWithContent() {
    assertThat(FileType.PNG.acceptsExtensionOf("photo.jpg")).isFalse();
    assertThat(FileType.PDF.acceptsExtensionOf("report.exe")).isFalse();
  }

  private static byte[] bytes(int... values) {
    byte[] result = new byte[values.length];
    for (int i = 0; i < values.length; i++) {
      result[i] = (byte) values[i];
    }
    return result;
  }
}
