package com.example.beinterviewprep.expense.service;

import com.example.beinterviewprep.expense.domain.Category;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseCommand(BigDecimal amount, Category category, LocalDate date, String note) {}
