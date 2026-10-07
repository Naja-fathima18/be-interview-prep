package com.example.beinterviewprep.expense.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;

public enum Category {
  FOOD,
  TRAVEL,
  BILLS,
  OTHER;

  @JsonCreator
  public static Category fromValue(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return Category.valueOf(value.trim().toUpperCase(Locale.ROOT));
  }
}
