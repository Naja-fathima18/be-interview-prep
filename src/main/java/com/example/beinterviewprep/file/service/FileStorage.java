package com.example.beinterviewprep.file.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FileStorage {

  private final Path baseDir;

  public FileStorage(FileStorageProperties properties) {
    this.baseDir = properties.baseDir().toAbsolutePath().normalize();
  }

  public void write(String storageKey, InputStream content) {
    Path target = resolve(storageKey);
    try {
      Files.createDirectories(baseDir);
      Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      deleteQuietly(storageKey);
      throw new UncheckedIOException("Could not store file " + storageKey, e);
    }
  }

  public Resource read(String storageKey) {
    return new FileSystemResource(resolve(storageKey));
  }

  public boolean exists(String storageKey) {
    return Files.isRegularFile(resolve(storageKey));
  }

  public void delete(String storageKey) {
    try {
      Files.deleteIfExists(resolve(storageKey));
    } catch (IOException e) {
      throw new UncheckedIOException("Could not delete file " + storageKey, e);
    }
  }

  public void deleteQuietly(String storageKey) {
    try {
      delete(storageKey);
    } catch (RuntimeException e) {
      log.warn("Could not clean up stored file {}", storageKey, e);
    }
  }

  Path resolve(String storageKey) {
    Path resolved = baseDir.resolve(storageKey).normalize();
    if (!resolved.startsWith(baseDir) || resolved.equals(baseDir)) {
      throw new IllegalArgumentException("Storage key escapes the storage directory");
    }
    return resolved;
  }
}
