package com.example.beinterviewprep.expense.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "expense")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Expense {

  public static final int MONEY_SCALE = 2;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, precision = 12, scale = MONEY_SCALE)
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Category category;

  @Column(name = "expense_date", nullable = false)
  private LocalDate date;

  @Column(length = 500)
  private String note;

  public Expense(BigDecimal amount, Category category, LocalDate date, String note) {
    update(amount, category, date, note);
  }

  public void update(BigDecimal amount, Category category, LocalDate date, String note) {
    this.amount = amount.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
    this.category = category;
    this.date = date;
    this.note = note;
  }
}
