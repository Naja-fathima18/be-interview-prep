package com.example.beinterviewprep.expense.persistence;

import com.example.beinterviewprep.expense.domain.Category;
import java.math.BigDecimal;

public record CategoryTotal(Category category, BigDecimal total) {}
