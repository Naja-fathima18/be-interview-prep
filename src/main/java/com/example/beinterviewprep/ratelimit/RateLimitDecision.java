package com.example.beinterviewprep.ratelimit;

import java.time.Duration;

public record RateLimitDecision(boolean allowed, int limit, int remaining, Duration retryAfter) {

  public long retryAfterSeconds() {
    long millis = retryAfter.toMillis();
    return Math.max(1, (millis + 999) / 1000);
  }
}
