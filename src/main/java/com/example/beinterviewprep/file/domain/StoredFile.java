package com.example.beinterviewprep.file.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stored_file")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoredFile {

  @Id private UUID id;

  @Column(name = "original_name", nullable = false)
  private String originalName;

  @Column(name = "content_type", nullable = false)
  private String contentType;

  @Column(name = "size_bytes", nullable = false)
  private long sizeBytes;

  @Column(name = "uploaded_at", nullable = false)
  private Instant uploadedAt;

  public StoredFile(
      UUID id, String originalName, String contentType, long sizeBytes, Instant uploadedAt) {
    this.id = id;
    this.originalName = originalName;
    this.contentType = contentType;
    this.sizeBytes = sizeBytes;
    this.uploadedAt = uploadedAt;
  }

  public String storageKey() {
    return id.toString();
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof StoredFile that)) {
      return false;
    }
    return id != null && id.equals(that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
