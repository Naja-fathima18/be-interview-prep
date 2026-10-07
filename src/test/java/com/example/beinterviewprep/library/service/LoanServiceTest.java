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
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

  @Mock BookRepository bookRepository;
  @Mock LoanRepository loanRepository;
  @InjectMocks LoanService loanService;

  private final Book book = new Book("Dune", "Frank Herbert", "9780441013593", 1965);

  @Test
  void borrowMarksBookUnavailableAndOpensLoan() {
    when(bookRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(book));
    when(loanRepository.saveAndFlush(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

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
    verify(loanRepository, never()).saveAndFlush(any());
  }

  @Test
  void translatesOpenLoanIndexViolationIntoConflict() {
    when(bookRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(book));
    when(loanRepository.saveAndFlush(any(Loan.class)))
        .thenThrow(integrityViolation("ux_loan_open_per_book"));

    assertThatThrownBy(() -> loanService.borrow(1L, 42L))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("already borrowed");
  }

  @Test
  void rethrowsUnrelatedIntegrityViolationOnBorrow() {
    when(bookRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(book));
    DataIntegrityViolationException unrelated = integrityViolation("fk_loan_book");
    when(loanRepository.saveAndFlush(any(Loan.class))).thenThrow(unrelated);

    assertThatThrownBy(() -> loanService.borrow(1L, 42L)).isSameAs(unrelated);
  }

  @Test
  void returnIsBasedOnTheOpenLoanEvenIfBookFlagDrifted() {
    Loan active = new Loan(book, 42L, Instant.now());
    when(bookRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(book));
    when(loanRepository.findByBookIdAndReturnedAtIsNull(1L)).thenReturn(Optional.of(active));

    Loan closed = loanService.giveBack(1L, 42L);

    assertThat(closed.getReturnedAt()).isNotNull();
    assertThat(book.isBorrowed()).isFalse();
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

    Loan closed = loanService.giveBack(1L, 42L);

    assertThat(book.isBorrowed()).isFalse();
    assertThat(closed.getReturnedAt()).isNotNull();
  }

  @Test
  void rejectsReturnByMemberWhoDoesNotHoldTheLoan() {
    book.markBorrowed();
    Loan active = new Loan(book, 42L, Instant.now());
    when(bookRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(book));
    when(loanRepository.findByBookIdAndReturnedAtIsNull(1L)).thenReturn(Optional.of(active));

    assertThatThrownBy(() -> loanService.giveBack(1L, 7L))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("not on loan to member 7");
    assertThat(active.getReturnedAt()).isNull();
    assertThat(book.isBorrowed()).isTrue();
  }

  @Test
  void rejectsReturningBookThatIsNotBorrowed() {
    when(bookRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(book));
    when(loanRepository.findByBookIdAndReturnedAtIsNull(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> loanService.giveBack(1L, 42L))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("not currently borrowed");
  }

  private static DataIntegrityViolationException integrityViolation(String constraint) {
    return new DataIntegrityViolationException(
        "violation",
        new ConstraintViolationException("violation", new SQLException("duplicate"), constraint));
  }
}
