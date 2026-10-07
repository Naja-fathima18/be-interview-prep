package com.example.beinterviewprep.common.error;

import org.springframework.http.HttpStatus;

public class BadRequestException extends ApiException {

  public BadRequestException(String detail) {
    super(HttpStatus.BAD_REQUEST, "Bad request", detail);
  }
}
