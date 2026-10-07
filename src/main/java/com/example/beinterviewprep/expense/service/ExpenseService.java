package com.example.beinterviewprep.expense.service;

import com.example.beinterviewprep.common.error.BadRequestException;
import com.example.beinterviewprep.common.error.NotFoundException;
import com.example.beinterviewprep.expense.api.ExpenseRequest;
import com.example.beinterviewprep.expense.api.ExpenseResponse;
import com.example.beinterviewprep.expense.api.MonthlySummaryResponse;
import com.example.beinterviewprep.expense.domain.Category;
import com.example.beinterviewprep.expense.domain.Expense;
import com.example.beinterviewprep.expense.persistence.CategoryTotal;
import com.example.beinterviewprep.expense.persistence.ExpenseRepository;
import com.example.beinterviewprep.expense.persistence.ExpenseSpecifications;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseService {

  private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(Expense.MONEY_SCALE);

  private final ExpenseRepository expenseRepository;

  @Transactional
  public ExpenseResponse create(ExpenseRequest request) {
    Expense expense =
        expenseRepository.save(
            new Expense(request.amount(), request.category(), request.date(), request.note()));
    log.info("Created expense {}", expense.getId());
    return ExpenseResponse.from(expense);
  }

  @Transactional(readOnly = true)
  public ExpenseResponse get(Long id) {
    return ExpenseResponse.from(findOrThrow(id));
  }

  @Transactional(readOnly = true)
  public Page<ExpenseResponse> list(
      LocalDate from, LocalDate to, Category category, Pageable pageable) {
    if (from != null && to != null && from.isAfter(to)) {
      throw new BadRequestException("Parameter 'from' must not be after 'to'");
    }
    return expenseRepository
        .findAll(ExpenseSpecifications.matching(from, to, category), pageable)
        .map(ExpenseResponse::from);
  }

  @Transactional
  public ExpenseResponse update(Long id, ExpenseRequest request) {
    Expense expense = findOrThrow(id);
    expense.update(request.amount(), request.category(), request.date(), request.note());
    return ExpenseResponse.from(expense);
  }

  @Transactional
  public void delete(Long id) {
    expenseRepository.delete(findOrThrow(id));
    log.info("Deleted expense {}", id);
  }

  @Transactional(readOnly = true)
  public MonthlySummaryResponse summarize(YearMonth month) {
    Map<Category, BigDecimal> totals = new EnumMap<>(Category.class);
    for (Category category : Category.values()) {
      totals.put(category, ZERO);
    }
    for (CategoryTotal row :
        expenseRepository.sumByCategoryBetween(month.atDay(1), month.atEndOfMonth())) {
      totals.put(row.category(), row.total().setScale(Expense.MONEY_SCALE));
    }
    BigDecimal overall = totals.values().stream().reduce(ZERO, BigDecimal::add);
    return new MonthlySummaryResponse(month, totals, overall);
  }

  private Expense findOrThrow(Long id) {
    return expenseRepository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Expense " + id + " not found"));
  }
}
