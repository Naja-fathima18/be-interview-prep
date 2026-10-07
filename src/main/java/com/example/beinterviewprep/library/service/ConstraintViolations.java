package com.example.beinterviewprep.library.service;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

final class ConstraintViolations {

  private ConstraintViolations() {}

  static boolean violates(DataIntegrityViolationException ex, String constraintName) {
    for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
      if (cause instanceof ConstraintViolationException violation
          && constraintName.equalsIgnoreCase(violation.getConstraintName())) {
        return true;
      }
    }
    return false;
  }
}
