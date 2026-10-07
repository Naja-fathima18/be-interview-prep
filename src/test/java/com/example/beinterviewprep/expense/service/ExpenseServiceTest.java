package com.example.beinterviewprep.expense.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.beinterviewprep.common.error.BadRequestException;
import com.example.beinterviewprep.common.error.NotFoundException;
import com.example.beinterviewprep.expense.domain.Category;
import com.example.beinterviewprep.expense.persistence.CategoryTotal;
import com.example.beinterviewprep.expense.persistence.ExpenseRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

  @Mock private ExpenseRepository repository;
  @InjectMocks private ExpenseService service;

  @Test
  void summarizesFromFirstToLastDayOfMonthIncludingLeapDay() {
    when(repository.sumByCategoryBetween(
            LocalDate.parse("2024-02-01"), LocalDate.parse("2024-02-29")))
        .thenReturn(List.of(new CategoryTotal(Category.FOOD, new BigDecimal("0.3"))));

    var summary = service.summarize(YearMonth.parse("2024-02"));

    assertThat(summary.month()).isEqualTo(YearMonth.parse("2024-02"));
    assertThat(summary.totalsByCategory())
        .containsOnlyKeys(Category.values())
        .containsEntry(Category.FOOD, new BigDecimal("0.30"))
        .containsEntry(Category.TRAVEL, new BigDecimal("0.00"))
        .containsEntry(Category.BILLS, new BigDecimal("0.00"))
        .containsEntry(Category.OTHER, new BigDecimal("0.00"));
    assertThat(summary.total()).isEqualTo(new BigDecimal("0.30"));
  }

  @Test
  void addsCategoryTotalsExactlyIntoOverallTotal() {
    when(repository.sumByCategoryBetween(
            LocalDate.parse("2026-02-01"), LocalDate.parse("2026-02-28")))
        .thenReturn(
            List.of(
                new CategoryTotal(Category.FOOD, new BigDecimal("0.10")),
                new CategoryTotal(Category.BILLS, new BigDecimal("0.20"))));

    var summary = service.summarize(YearMonth.parse("2026-02"));

    assertThat(summary.total()).isEqualTo(new BigDecimal("0.30"));
  }

  @Test
  void rejectsListingWhenFromIsAfterTo() {
    assertThatThrownBy(
            () ->
                service.list(
                    LocalDate.parse("2026-03-02"),
                    LocalDate.parse("2026-03-01"),
                    null,
                    Pageable.unpaged()))
        .isInstanceOf(BadRequestException.class);
    verifyNoInteractions(repository);
  }

  @Test
  void rejectsSortingByUnknownProperty() {
    Pageable pageable = PageRequest.of(0, 10, Sort.by("foo"));

    assertThatThrownBy(() -> service.list(null, null, null, pageable))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("foo");
    verifyNoInteractions(repository);
  }

  @Test
  @SuppressWarnings("unchecked")
  void appendsIdTieBreakerToRequestedSort() {
    when(repository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(Page.empty());

    service.list(null, null, null, PageRequest.of(2, 10, Sort.by("date").descending()));

    verify(repository)
        .findAll(
            any(Specification.class),
            eq(PageRequest.of(2, 10, Sort.by(Sort.Order.desc("date"), Sort.Order.desc("id")))));
  }

  @Test
  void throwsNotFoundForUnknownExpense() {
    when(repository.findById(42L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.get(42L)).isInstanceOf(NotFoundException.class);
  }
}
