package com.example.beinterviewprep.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.util.unit.DataSize;

class FileStorageTest {

  @TempDir Path tempDir;

  @Test
  void writesReadsAndDeletesInsideBaseDirectory() throws Exception {
    FileStorage storage = storageIn(tempDir.resolve("files"));

    storage.write("key-1", new ByteArrayInputStream(new byte[] {1, 2, 3}));

    assertThat(Files.readAllBytes(tempDir.resolve("files/key-1"))).containsExactly(1, 2, 3);
    assertThat(storage.read("key-1").getContentAsByteArray()).containsExactly(1, 2, 3);

    storage.delete("key-1");

    assertThat(storage.exists("key-1")).isFalse();
  }

  @Test
  void refusesKeysThatEscapeBaseDirectory() {
    FileStorage storage = storageIn(tempDir.resolve("files"));

    assertThatThrownBy(() -> storage.write("../outside", new ByteArrayInputStream(new byte[1])))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> storage.read("../../etc/passwd"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> storage.delete(".")).isInstanceOf(IllegalArgumentException.class);
    assertThat(tempDir.resolve("outside")).doesNotExist();
  }

  private static FileStorage storageIn(Path dir) {
    return new FileStorage(new FileStorageProperties(dir, DataSize.ofMegabytes(5)));
  }
}
