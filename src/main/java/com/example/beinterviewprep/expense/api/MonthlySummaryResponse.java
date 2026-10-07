package com.example.beinterviewprep.expense.api;

import com.example.beinterviewprep.expense.domain.Category;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;

public record MonthlySummaryResponse(
    YearMonth month, Map<Category, BigDecimal> totalsByCategory, BigDecimal total) {}
