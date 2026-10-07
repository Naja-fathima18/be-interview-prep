package com.example.beinterviewprep.library.api;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.Year;

public class NotFutureYearValidator implements ConstraintValidator<NotFutureYear, Integer> {

  @Override
  public boolean isValid(Integer year, ConstraintValidatorContext context) {
    return year == null || year <= Year.now().getValue();
  }
}
