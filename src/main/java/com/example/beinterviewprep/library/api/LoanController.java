package com.example.beinterviewprep.library.api;

import com.example.beinterviewprep.library.service.LoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/books/{bookId}")
@RequiredArgsConstructor
public class LoanController {

  private final LoanService loanService;

  @PostMapping("/borrow")
  @ResponseStatus(HttpStatus.CREATED)
  public LoanResponse borrow(@PathVariable Long bookId, @Valid @RequestBody BorrowRequest request) {
    return LoanResponse.from(loanService.borrow(bookId, request.memberId()));
  }

  @PostMapping("/return")
  public LoanResponse giveBack(
      @PathVariable Long bookId, @Valid @RequestBody ReturnRequest request) {
    return LoanResponse.from(loanService.giveBack(bookId, request.memberId()));
  }
}
