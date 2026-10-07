package com.example.beinterviewprep.library.api;

import com.example.beinterviewprep.library.domain.Loan;
import java.time.Instant;

public record LoanResponse(
    Long id, Long bookId, Long memberId, Instant borrowedAt, Instant returnedAt) {

  public static LoanResponse from(Loan loan) {
    return new LoanResponse(
        loan.getId(),
        loan.getBook().getId(),
        loan.getMemberId(),
        loan.getBorrowedAt(),
        loan.getReturnedAt());
  }
}
