package com.example.beinterviewprep.expense.api;

import com.example.beinterviewprep.expense.domain.Category;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseRequest(
    @NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
    @NotNull Category category,
    @NotNull LocalDate date,
    @Size(max = 500) String note) {}
