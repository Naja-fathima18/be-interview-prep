package com.example.beinterviewprep.file.service;

import com.example.beinterviewprep.common.error.BadRequestException;
import com.example.beinterviewprep.common.error.NotFoundException;
import com.example.beinterviewprep.file.domain.FileType;
import com.example.beinterviewprep.file.domain.StoredFile;
import com.example.beinterviewprep.file.persistence.StoredFileRepository;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

  private final StoredFileRepository repository;
  private final FileStorage storage;
  private final FileStorageProperties properties;

  @Transactional
  public StoredFileView upload(MultipartFile upload) {
    if (upload == null || upload.isEmpty()) {
      throw new BadRequestException("File is empty");
    }
    if (upload.getSize() > properties.maxFileSize().toBytes()) {
      throw new FileTooLargeException(
          "File exceeds the maximum size of " + properties.maxFileSize().toMegabytes() + " MB");
    }
    String originalName = FileNames.sanitize(upload.getOriginalFilename());
    FileType type = detectType(upload, originalName);

    StoredFile file =
        new StoredFile(
            UUID.randomUUID(), originalName, type.mediaType(), upload.getSize(), Instant.now());
    writeContent(upload, file);
    deleteContentOnRollback(file.storageKey());
    repository.saveAndFlush(file);
    log.info("Stored file {} ({}, {} bytes)", file.getId(), type, file.getSizeBytes());
    return StoredFileView.from(file);
  }

  @Transactional(readOnly = true)
  public Page<StoredFileView> list(Pageable pageable) {
    return repository.findAll(pageable).map(StoredFileView::from);
  }

  @Transactional(readOnly = true)
  public StoredFileView get(UUID id) {
    return StoredFileView.from(findOrThrow(id));
  }

  @Transactional(readOnly = true)
  public FileDownload download(UUID id) {
    StoredFile file = findOrThrow(id);
    if (!storage.exists(file.storageKey())) {
      log.error("Stored content missing for file {}", id);
      throw new NotFoundException("Content of file " + id + " is not available");
    }
    return new FileDownload(StoredFileView.from(file), storage.read(file.storageKey()));
  }

  @Transactional
  public void delete(UUID id) {
    StoredFile file = findOrThrow(id);
    repository.delete(file);
    repository.flush();
    storage.delete(file.storageKey());
    log.info("Deleted file {}", id);
  }

  private StoredFile findOrThrow(UUID id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("File " + id + " not found"));
  }

  private FileType detectType(MultipartFile upload, String originalName) {
    FileType type =
        FileType.detect(readHeader(upload))
            .orElseThrow(
                () ->
                    new UnsupportedFileTypeException(
                        "Only JPEG, PNG and PDF files are allowed; the file content is none of"
                            + " these"));
    if (!type.acceptsExtensionOf(originalName)) {
      throw new UnsupportedFileTypeException(
          "File extension of '" + originalName + "' does not match its " + type + " content");
    }
    return type;
  }

  private byte[] readHeader(MultipartFile upload) {
    try (InputStream content = upload.getInputStream()) {
      return content.readNBytes(FileType.SIGNATURE_LENGTH);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void deleteContentOnRollback(String storageKey) {
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            if (status == STATUS_ROLLED_BACK) {
              storage.deleteQuietly(storageKey);
            }
          }
        });
  }

  private void writeContent(MultipartFile upload, StoredFile file) {
    try (InputStream content = upload.getInputStream()) {
      storage.write(file.storageKey(), content);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
