package com.example.beinterviewprep.expense.api;

import com.example.beinterviewprep.expense.domain.Category;
import com.example.beinterviewprep.expense.domain.Expense;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseResponse(
    Long id, BigDecimal amount, Category category, LocalDate date, String note) {

  public static ExpenseResponse from(Expense expense) {
    return new ExpenseResponse(
        expense.getId(),
        expense.getAmount(),
        expense.getCategory(),
        expense.getDate(),
        expense.getNote());
  }
}
