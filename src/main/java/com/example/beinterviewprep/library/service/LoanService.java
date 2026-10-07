package com.example.beinterviewprep.library.service;

import com.example.beinterviewprep.common.error.ConflictException;
import com.example.beinterviewprep.common.error.NotFoundException;
import com.example.beinterviewprep.library.domain.Book;
import com.example.beinterviewprep.library.domain.Loan;
import com.example.beinterviewprep.library.persistence.BookRepository;
import com.example.beinterviewprep.library.persistence.LoanRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanService {

  private static final String OPEN_LOAN_CONSTRAINT = "ux_loan_open_per_book";

  private final BookRepository bookRepository;
  private final LoanRepository loanRepository;

  @Transactional
  public Loan borrow(Long bookId, Long memberId) {
    Book book = lockBook(bookId);
    if (book.isBorrowed()) {
      throw alreadyBorrowed(bookId);
    }
    book.markBorrowed();
    Loan loan = saveEnforcingOneOpenLoan(new Loan(book, memberId, Instant.now()));
    log.info("Book {} borrowed by member {} as loan {}", bookId, memberId, loan.getId());
    return loan;
  }

  @Transactional
  public Loan giveBack(Long bookId, Long memberId) {
    Book book = lockBook(bookId);
    Loan loan =
        loanRepository
            .findByBookIdAndReturnedAtIsNull(bookId)
            .orElseThrow(
                () -> new ConflictException("Book " + bookId + " is not currently borrowed"));
    if (!loan.getMemberId().equals(memberId)) {
      throw new ConflictException(
          "Book " + bookId + " is not on loan to member " + memberId + " and cannot be returned");
    }
    book.markReturned();
    loan.close(Instant.now());
    log.info("Book {} returned, loan {} closed", bookId, loan.getId());
    return loan;
  }

  private Loan saveEnforcingOneOpenLoan(Loan loan) {
    try {
      return loanRepository.saveAndFlush(loan);
    } catch (DataIntegrityViolationException ex) {
      if (ConstraintViolations.violates(ex, OPEN_LOAN_CONSTRAINT)) {
        throw alreadyBorrowed(loan.getBook().getId());
      }
      throw ex;
    }
  }

  private static ConflictException alreadyBorrowed(Long bookId) {
    return new ConflictException(
        "Book " + bookId + " is already borrowed and is unavailable until it is returned");
  }

  private Book lockBook(Long bookId) {
    return bookRepository
        .findByIdForUpdate(bookId)
        .orElseThrow(() -> new NotFoundException("Book " + bookId + " not found"));
  }
}
