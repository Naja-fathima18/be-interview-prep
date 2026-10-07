package com.example.beinterviewprep.library.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.beinterviewprep.common.error.ConflictException;
import com.example.beinterviewprep.library.api.BookRequest;
import com.example.beinterviewprep.library.domain.Book;
import com.example.beinterviewprep.library.persistence.BookRepository;
import java.sql.SQLException;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

  @Mock BookRepository bookRepository;
  @InjectMocks BookService bookService;

  private final BookRequest request =
      new BookRequest("Dune", "Frank Herbert", "9780441013593", 1965);

  @Test
  void rejectsCreatingBookWithExistingIsbn() {
    when(bookRepository.existsByIsbn("9780441013593")).thenReturn(true);

    assertThatThrownBy(() -> bookService.create(request))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("9780441013593");
    verify(bookRepository, never()).saveAndFlush(any());
  }

  @Test
  void translatesUniqueConstraintRaceIntoConflict() {
    when(bookRepository.existsByIsbn("9780441013593")).thenReturn(false);
    when(bookRepository.saveAndFlush(any())).thenThrow(integrityViolation("uk_book_isbn"));

    assertThatThrownBy(() -> bookService.create(request)).isInstanceOf(ConflictException.class);
  }

  @Test
  void rethrowsIntegrityViolationOfOtherConstraints() {
    when(bookRepository.existsByIsbn("9780441013593")).thenReturn(false);
    DataIntegrityViolationException unrelated = integrityViolation("book_title_not_null");
    when(bookRepository.saveAndFlush(any())).thenThrow(unrelated);

    assertThatThrownBy(() -> bookService.create(request)).isSameAs(unrelated);
  }

  @Test
  void rejectsDeletingBorrowedBook() {
    Book book = new Book("Dune", "Frank Herbert", "9780441013593", 1965);
    book.markBorrowed();
    when(bookRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(book));

    assertThatThrownBy(() -> bookService.delete(1L))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("currently borrowed");
    verify(bookRepository, never()).delete(any());
  }

  private static DataIntegrityViolationException integrityViolation(String constraint) {
    return new DataIntegrityViolationException(
        "violation",
        new ConstraintViolationException("violation", new SQLException("violation"), constraint));
  }
}
