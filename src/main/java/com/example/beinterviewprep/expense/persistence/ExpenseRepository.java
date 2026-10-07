package com.example.beinterviewprep.expense.persistence;

import com.example.beinterviewprep.expense.domain.Expense;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository
    extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {

  @Query(
      """
      select new com.example.beinterviewprep.expense.persistence.CategoryTotal(
          e.category, sum(e.amount))
      from Expense e
      where e.date between :from and :to
      group by e.category
      """)
  List<CategoryTotal> sumByCategoryBetween(
      @Param("from") LocalDate from, @Param("to") LocalDate to);
}
