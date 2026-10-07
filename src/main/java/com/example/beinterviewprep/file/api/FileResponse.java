package com.example.beinterviewprep.file.api;

import com.example.beinterviewprep.file.service.StoredFileView;
import java.time.Instant;
import java.util.UUID;

public record FileResponse(
    UUID id, String originalName, String contentType, long size, Instant uploadedAt) {

  static FileResponse from(StoredFileView view) {
    return new FileResponse(
        view.id(), view.originalName(), view.contentType(), view.size(), view.uploadedAt());
  }
}
