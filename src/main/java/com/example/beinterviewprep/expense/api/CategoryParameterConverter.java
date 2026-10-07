package com.example.beinterviewprep.expense.api;

import com.example.beinterviewprep.expense.domain.Category;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
class CategoryParameterConverter implements Converter<String, Category> {

  @Override
  public Category convert(@NonNull String source) {
    return Category.fromValue(source);
  }
}
