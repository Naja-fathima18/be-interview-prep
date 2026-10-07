package com.example.beinterviewprep.file.service;

import com.example.beinterviewprep.common.error.ApiException;
import org.springframework.http.HttpStatus;

public class UnsupportedFileTypeException extends ApiException {

  public UnsupportedFileTypeException(String detail) {
    super(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported file type", detail);
  }
}
