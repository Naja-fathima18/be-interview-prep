package com.example.beinterviewprep.expense.api;

import java.util.List;
import org.springframework.data.domain.Page;

public record ExpensePageResponse(
    List<ExpenseResponse> content, int page, int size, long totalElements, int totalPages) {

  public static ExpensePageResponse from(Page<ExpenseResponse> page) {
    return new ExpensePageResponse(
        page.getContent(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages());
  }
}
