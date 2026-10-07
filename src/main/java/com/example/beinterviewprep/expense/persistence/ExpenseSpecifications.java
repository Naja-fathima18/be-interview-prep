package com.example.beinterviewprep.expense.persistence;

import com.example.beinterviewprep.expense.domain.Category;
import com.example.beinterviewprep.expense.domain.Expense;
import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

public final class ExpenseSpecifications {

  private ExpenseSpecifications() {}

  public static Specification<Expense> matching(LocalDate from, LocalDate to, Category category) {
    return Specification.allOf(onOrAfter(from), onOrBefore(to), inCategory(category));
  }

  private static Specification<Expense> onOrAfter(LocalDate from) {
    return (root, query, cb) ->
        from == null ? null : cb.greaterThanOrEqualTo(root.get("date"), from);
  }

  private static Specification<Expense> onOrBefore(LocalDate to) {
    return (root, query, cb) -> to == null ? null : cb.lessThanOrEqualTo(root.get("date"), to);
  }

  private static Specification<Expense> inCategory(Category category) {
    return (root, query, cb) -> category == null ? null : cb.equal(root.get("category"), category);
  }
}
