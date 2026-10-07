package com.example.beinterviewprep.expense.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.beinterviewprep.expense.domain.Category;
import com.example.beinterviewprep.expense.domain.Expense;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class ExpenseRepositoryTest {

  @Autowired private ExpenseRepository repository;

  @Test
  void sumsOnlyExpensesWithinTheInclusiveRangeGroupedByCategory() {
    save("5.00", Category.FOOD, "2024-01-31");
    save("0.10", Category.FOOD, "2024-02-01");
    save("0.20", Category.FOOD, "2024-02-29");
    save("12.34", Category.BILLS, "2024-02-15");
    save("7.00", Category.BILLS, "2024-03-01");

    var totals =
        repository.sumByCategoryBetween(
            LocalDate.parse("2024-02-01"), LocalDate.parse("2024-02-29"));

    assertThat(totals)
        .containsExactlyInAnyOrder(
            new CategoryTotal(Category.FOOD, new BigDecimal("0.30")),
            new CategoryTotal(Category.BILLS, new BigDecimal("12.34")));
  }

  @Test
  void filtersByInclusiveDateRangeAndCategory() {
    save("1.00", Category.TRAVEL, "2026-03-09");
    Expense first = save("2.00", Category.TRAVEL, "2026-03-10");
    Expense last = save("3.00", Category.TRAVEL, "2026-03-12");
    save("4.00", Category.FOOD, "2026-03-11");
    save("5.00", Category.TRAVEL, "2026-03-13");

    var result =
        repository.findAll(
            ExpenseSpecifications.matching(
                LocalDate.parse("2026-03-10"), LocalDate.parse("2026-03-12"), Category.TRAVEL));

    assertThat(result)
        .extracting(Expense::getId)
        .containsExactlyInAnyOrder(first.getId(), last.getId());
  }

  @Test
  void returnsEverythingWhenNoFilterIsGiven() {
    save("1.00", Category.TRAVEL, "2026-03-09");
    save("4.00", Category.FOOD, "2026-03-11");

    assertThat(repository.findAll(ExpenseSpecifications.matching(null, null, null))).hasSize(2);
  }

  private Expense save(String amount, Category category, String date) {
    return repository.saveAndFlush(
        new Expense(new BigDecimal(amount), category, LocalDate.parse(date), null));
  }
}
