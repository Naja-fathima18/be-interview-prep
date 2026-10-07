package com.example.beinterviewprep.file.service;

import com.example.beinterviewprep.file.domain.StoredFile;
import java.time.Instant;
import java.util.UUID;

public record StoredFileView(
    UUID id, String originalName, String contentType, long size, Instant uploadedAt) {

  static StoredFileView from(StoredFile file) {
    return new StoredFileView(
        file.getId(),
        file.getOriginalName(),
        file.getContentType(),
        file.getSizeBytes(),
        file.getUploadedAt());
  }
}
