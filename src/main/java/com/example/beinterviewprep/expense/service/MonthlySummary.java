package com.example.beinterviewprep.expense.service;

import com.example.beinterviewprep.expense.domain.Category;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;

public record MonthlySummary(
    YearMonth month, Map<Category, BigDecimal> totalsByCategory, BigDecimal total) {}
