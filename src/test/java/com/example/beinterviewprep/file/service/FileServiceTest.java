package com.example.beinterviewprep.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.beinterviewprep.file.domain.StoredFile;
import com.example.beinterviewprep.file.persistence.StoredFileRepository;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.unit.DataSize;

class FileServiceTest {

  private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1};

  private final StoredFileRepository repository = mock(StoredFileRepository.class);
  private final FileStorage storage = mock(FileStorage.class);
  private final FileService service =
      new FileService(
          repository,
          storage,
          new FileStorageProperties(Path.of("unused"), DataSize.ofMegabytes(5)));

  @BeforeEach
  void startTransactionSynchronization() {
    TransactionSynchronizationManager.initSynchronization();
  }

  @AfterEach
  void clearTransactionSynchronization() {
    TransactionSynchronizationManager.clearSynchronization();
  }

  @Test
  void deletesStoredContentWhenSavingRecordFailsAndTransactionRollsBack() {
    when(repository.saveAndFlush(any())).thenThrow(new DataAccessResourceFailureException("down"));

    assertThatThrownBy(() -> service.upload(png("photo.png")))
        .isInstanceOf(DataAccessResourceFailureException.class);
    completeTransaction(TransactionSynchronization.STATUS_ROLLED_BACK);

    String storageKey = writtenStorageKey();
    verify(storage).deleteQuietly(storageKey);
  }

  @Test
  void keepsStoredContentWhenTransactionCommits() {
    when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

    StoredFileView view = service.upload(png("photo.png"));
    completeTransaction(TransactionSynchronization.STATUS_COMMITTED);

    assertThat(writtenStorageKey()).isEqualTo(view.id().toString());
    verify(storage, never()).deleteQuietly(anyString());
  }

  @Test
  void neverWritesContentForRejectedUpload() {
    MockMultipartFile exe =
        new MockMultipartFile("file", "photo.png", "image/png", new byte[] {0x4D, 0x5A, 0, 0});

    assertThatThrownBy(() -> service.upload(exe)).isInstanceOf(UnsupportedFileTypeException.class);

    verify(storage, never()).write(anyString(), any());
    verify(repository, never()).saveAndFlush(any());
  }

  @Test
  void deletesRecordBeforeContentAndPropagatesContentDeleteFailure() {
    StoredFile file = new StoredFile(UUID.randomUUID(), "a.png", "image/png", 9, Instant.now());
    when(repository.findById(file.getId())).thenReturn(Optional.of(file));
    doThrow(new UncheckedIOException(new IOException("locked")))
        .when(storage)
        .delete(file.storageKey());

    assertThatThrownBy(() -> service.delete(file.getId())).isInstanceOf(UncheckedIOException.class);

    InOrder order = inOrder(repository, storage);
    order.verify(repository).delete(file);
    order.verify(repository).flush();
    order.verify(storage).delete(file.storageKey());
  }

  private String writtenStorageKey() {
    ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
    verify(storage).write(key.capture(), any(InputStream.class));
    return key.getValue();
  }

  private static void completeTransaction(int status) {
    TransactionSynchronizationManager.getSynchronizations()
        .forEach(synchronization -> synchronization.afterCompletion(status));
  }

  private static MockMultipartFile png(String name) {
    return new MockMultipartFile("file", name, "image/png", PNG);
  }
}
