package com.example.beinterviewprep.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

public class SlidingWindowRateLimiter {

  private final int limit;
  private final Duration window;
  private final Clock clock;
  private final ConcurrentMap<String, RequestLog> logs = new ConcurrentHashMap<>();
  private final AtomicReference<Instant> lastEviction;

  public SlidingWindowRateLimiter(int limit, Duration window, Clock clock) {
    this.limit = limit;
    this.window = window;
    this.clock = clock;
    this.lastEviction = new AtomicReference<>(clock.instant());
  }

  public RateLimitDecision tryAcquire(String key) {
    Instant now = clock.instant();
    evictIdleKeysIfDue(now);
    AtomicReference<RateLimitDecision> decision = new AtomicReference<>();
    logs.compute(
        key,
        (k, existing) -> {
          RequestLog log = existing == null ? new RequestLog() : existing;
          decision.set(log.record(now));
          return log;
        });
    return decision.get();
  }

  int trackedKeys() {
    return logs.size();
  }

  private void evictIdleKeysIfDue(Instant now) {
    Instant previous = lastEviction.get();
    if (now.isBefore(previous.plus(window)) || !lastEviction.compareAndSet(previous, now)) {
      return;
    }
    for (String key : logs.keySet()) {
      logs.computeIfPresent(key, (k, log) -> log.isIdle(now) ? null : log);
    }
  }

  private final class RequestLog {

    private final Deque<Instant> timestamps = new ArrayDeque<>(limit);

    RateLimitDecision record(Instant now) {
      dropExpired(now);
      if (timestamps.size() < limit) {
        timestamps.addLast(now);
        return new RateLimitDecision(true, limit, limit - timestamps.size(), Duration.ZERO);
      }
      Duration retryAfter = Duration.between(now, timestamps.peekFirst().plus(window));
      return new RateLimitDecision(false, limit, 0, retryAfter);
    }

    boolean isIdle(Instant now) {
      Instant newest = timestamps.peekLast();
      return newest == null || isExpired(newest, now);
    }

    private void dropExpired(Instant now) {
      while (!timestamps.isEmpty() && isExpired(timestamps.peekFirst(), now)) {
        timestamps.removeFirst();
      }
    }

    private boolean isExpired(Instant timestamp, Instant now) {
      return !timestamp.plus(window).isAfter(now);
    }
  }
}
