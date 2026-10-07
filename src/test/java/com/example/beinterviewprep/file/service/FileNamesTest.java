package com.example.beinterviewprep.file.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FileNamesTest {

  @Test
  void stripsUnixAndWindowsPathComponents() {
    assertThat(FileNames.sanitize("../../etc/passwd.png")).isEqualTo("passwd.png");
    assertThat(FileNames.sanitize("..\\..\\windows\\evil.pdf")).isEqualTo("evil.pdf");
    assertThat(FileNames.sanitize("/abs/path/photo.jpg")).isEqualTo("photo.jpg");
  }

  @Test
  void fallsBackForMissingOrDotOnlyNames() {
    assertThat(FileNames.sanitize(null)).isEqualTo(FileNames.FALLBACK_NAME);
    assertThat(FileNames.sanitize("")).isEqualTo(FileNames.FALLBACK_NAME);
    assertThat(FileNames.sanitize("..")).isEqualTo(FileNames.FALLBACK_NAME);
    assertThat(FileNames.sanitize("dir/")).isEqualTo(FileNames.FALLBACK_NAME);
  }

  @Test
  void removesControlCharactersAndKeepsUnicode() {
    assertThat(FileNames.sanitize("re\u0000port\n.pdf")).isEqualTo("report.pdf");
    assertThat(FileNames.sanitize("résumé.pdf")).isEqualTo("résumé.pdf");
  }

  @Test
  void truncatesOverlongNamesKeepingTheExtension() {
    String sanitized = FileNames.sanitize("a".repeat(300) + ".png");

    assertThat(sanitized).hasSize(255).endsWith(".png");
  }
}
