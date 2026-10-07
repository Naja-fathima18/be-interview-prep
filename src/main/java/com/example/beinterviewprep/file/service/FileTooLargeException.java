package com.example.beinterviewprep.file.service;

import com.example.beinterviewprep.common.error.ApiException;
import org.springframework.http.HttpStatus;

public class FileTooLargeException extends ApiException {

  public FileTooLargeException(String detail) {
    super(HttpStatus.PAYLOAD_TOO_LARGE, "File too large", detail);
  }
}
