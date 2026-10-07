package com.example.beinterviewprep.expense.service;

import com.example.beinterviewprep.common.error.BadRequestException;
import com.example.beinterviewprep.common.error.NotFoundException;
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
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseService {

  private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(Expense.MONEY_SCALE);

  private static final Set<String> SORTABLE_PROPERTIES = Set.of("id", "date", "amount", "category");
  private static final String TIE_BREAKER = "id";

  private final ExpenseRepository expenseRepository;

  @Transactional
  public Expense create(ExpenseCommand command) {
    Expense expense =
        expenseRepository.save(
            new Expense(command.amount(), command.category(), command.date(), command.note()));
    log.info("Created expense {}", expense.getId());
    return expense;
  }

  @Transactional(readOnly = true)
  public Expense get(Long id) {
    return findOrThrow(id);
  }

  @Transactional(readOnly = true)
  public Page<Expense> list(LocalDate from, LocalDate to, Category category, Pageable pageable) {
    if (from != null && to != null && from.isAfter(to)) {
      throw new BadRequestException("Parameter 'from' must not be after 'to'");
    }
    return expenseRepository.findAll(
        ExpenseSpecifications.matching(from, to, category), withStableSort(pageable));
  }

  @Transactional
  public Expense update(Long id, ExpenseCommand command) {
    Expense expense = findOrThrow(id);
    expense.update(command.amount(), command.category(), command.date(), command.note());
    return expense;
  }

  @Transactional
  public void delete(Long id) {
    expenseRepository.delete(findOrThrow(id));
    log.info("Deleted expense {}", id);
  }

  @Transactional(readOnly = true)
  public MonthlySummary summarize(YearMonth month) {
    Map<Category, BigDecimal> totals = new EnumMap<>(Category.class);
    for (Category category : Category.values()) {
      totals.put(category, ZERO);
    }
    for (CategoryTotal row :
        expenseRepository.sumByCategoryBetween(month.atDay(1), month.atEndOfMonth())) {
      totals.put(row.category(), row.total().setScale(Expense.MONEY_SCALE));
    }
    BigDecimal overall = totals.values().stream().reduce(ZERO, BigDecimal::add);
    return new MonthlySummary(month, totals, overall);
  }

  private static Pageable withStableSort(Pageable pageable) {
    Sort sort = pageable.getSort();
    for (Sort.Order order : sort) {
      if (!SORTABLE_PROPERTIES.contains(order.getProperty())) {
        throw new BadRequestException(
            "Cannot sort by '" + order.getProperty() + "'; allowed: " + SORTABLE_PROPERTIES);
      }
    }
    if (sort.getOrderFor(TIE_BREAKER) == null) {
      sort = sort.and(Sort.by(TIE_BREAKER).descending());
    }
    return pageable.isPaged()
        ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort)
        : Pageable.unpaged(sort);
  }

  private Expense findOrThrow(Long id) {
    return expenseRepository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Expense " + id + " not found"));
  }
}
