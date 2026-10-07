package com.example.beinterviewprep.library.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.beinterviewprep.common.error.ConflictException;
import com.example.beinterviewprep.common.error.NotFoundException;
import com.example.beinterviewprep.library.domain.Book;
import com.example.beinterviewprep.library.domain.Loan;
import com.example.beinterviewprep.library.persistence.BookRepository;
import com.example.beinterviewprep.library.persistence.LoanRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

  @Mock BookRepository bookRepository;
  @Mock LoanRepository loanRepository;
  @InjectMocks LoanService loanService;

  private final Book book = new Book("Dune", "Frank Herbert", "9780441013593", 1965);

  @Test
  void borrowMarksBookUnavailableAndOpensLoan() {
    when(bookRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(book));
    when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

    Loan loan = loanService.borrow(1L, 42L);

    assertThat(book.isBorrowed()).isTrue();
    assertThat(loan.getMemberId()).isEqualTo(42L);
    assertThat(loan.getReturnedAt()).isNull();
  }

  @Test
  void rejectsBorrowingBookThatIsAlreadyBorrowed() {
    book.markBorrowed();
    when(bookRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(book));

    assertThatThrownBy(() -> loanService.borrow(1L, 42L))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("already borrowed");
    verify(loanRepository, never()).save(any());
  }

  @Test
  void rejectsBorrowingUnknownBook() {
    when(bookRepository.findByIdForUpdate(9L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> loanService.borrow(9L, 42L)).isInstanceOf(NotFoundException.class);
  }

  @Test
  void returnClosesActiveLoanAndMakesBookAvailable() {
    book.markBorrowed();
    Loan active = new Loan(book, 42L, Instant.now());
    when(bookRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(book));
    when(loanRepository.findByBookIdAndReturnedAtIsNull(1L)).thenReturn(Optional.of(active));

    Loan closed = loanService.giveBack(1L);

    assertThat(book.isBorrowed()).isFalse();
    assertThat(closed.getReturnedAt()).isNotNull();
  }

  @Test
  void rejectsReturningBookThatIsNotBorrowed() {
    when(bookRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(book));
    when(loanRepository.findByBookIdAndReturnedAtIsNull(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> loanService.giveBack(1L))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("not currently borrowed");
  }
}
