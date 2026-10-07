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

  @Test
  void truncatesByCodePointsWithoutSplittingSurrogatePairs() {
    String emoji = "\uD83D\uDE00";
    String sanitized = FileNames.sanitize(emoji.repeat(300) + ".png");

    assertThat(sanitized.codePointCount(0, sanitized.length()))
        .isEqualTo(FileNames.MAX_CODE_POINTS);
    assertThat(sanitized).startsWith(emoji).endsWith(".png");
    assertThat(Character.isLowSurrogate(sanitized.charAt(0))).isFalse();
  }

  @Test
  void stripsBidiOverridesThatDisguiseTheExtension() {
    assertThat(FileNames.sanitize("invoice\u202Egnp.exe")).isEqualTo("invoicegnp.exe");
    assertThat(FileNames.sanitize("a\u202Ab\u202Bc\u202Cd\u202De.pdf")).isEqualTo("abcde.pdf");
    assertThat(FileNames.sanitize("x\u2066y\u2067z\u2068w\u2069.png")).isEqualTo("xyzw.png");
  }

  @Test
  void stripsUnicodeLineAndParagraphSeparators() {
    assertThat(FileNames.sanitize("line\u2028break\u2029.jpg")).isEqualTo("linebreak.jpg");
  }

  @Test
  void fallsBackWhenOnlyUnsafeCharactersRemain() {
    assertThat(FileNames.sanitize("\u202E\u2066\u2028")).isEqualTo(FileNames.FALLBACK_NAME);
  }
}
