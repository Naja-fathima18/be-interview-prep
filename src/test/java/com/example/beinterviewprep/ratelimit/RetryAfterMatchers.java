package com.example.beinterviewprep.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultMatcher;

final class RetryAfterMatchers {

  private static final long SLOW_RUN_TOLERANCE_SECONDS = 5;

  private RetryAfterMatchers() {}

  static ResultMatcher retryAfterCloseTo(long windowSeconds) {
    long lowest = Math.max(1, windowSeconds - SLOW_RUN_TOLERANCE_SECONDS);
    return result -> {
      long header = Long.parseLong(result.getResponse().getHeader(HttpHeaders.RETRY_AFTER));
      Number body = JsonPath.read(result.getResponse().getContentAsString(), "$.retryAfterSeconds");
      assertThat(header).isBetween(lowest, windowSeconds);
      assertThat(body.longValue()).isEqualTo(header);
    };
  }
}
