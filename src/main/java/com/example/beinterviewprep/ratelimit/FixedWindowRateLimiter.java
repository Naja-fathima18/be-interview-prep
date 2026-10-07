package com.example.beinterviewprep.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

public class FixedWindowRateLimiter {

  private final int limit;
  private final Duration window;
  private final Clock clock;
  private final ConcurrentMap<String, Window> windows = new ConcurrentHashMap<>();
  private final AtomicReference<Instant> lastEviction;

  public FixedWindowRateLimiter(int limit, Duration window, Clock clock) {
    this.limit = limit;
    this.window = window;
    this.clock = clock;
    this.lastEviction = new AtomicReference<>(clock.instant());
  }

  public RateLimitDecision tryAcquire(String key) {
    Instant now = clock.instant();
    evictExpiredWindowsIfDue(now);
    Window current = windows.compute(key, (k, existing) -> next(existing, now));
    int remaining = Math.max(0, limit - current.count());
    Duration retryAfter = current.allowed() ? Duration.ZERO : current.timeUntilReset(now, window);
    return new RateLimitDecision(current.allowed(), limit, remaining, retryAfter);
  }

  int trackedKeys() {
    return windows.size();
  }

  private Window next(Window existing, Instant now) {
    if (existing == null || existing.isExpired(now, window)) {
      return new Window(now, 1, true);
    }
    if (existing.count() < limit) {
      return new Window(existing.start(), existing.count() + 1, true);
    }
    return new Window(existing.start(), existing.count(), false);
  }

  private void evictExpiredWindowsIfDue(Instant now) {
    Instant previous = lastEviction.get();
    if (now.isBefore(previous.plus(window)) || !lastEviction.compareAndSet(previous, now)) {
      return;
    }
    windows.values().removeIf(w -> w.isExpired(now, window));
  }

  private record Window(Instant start, int count, boolean allowed) {

    boolean isExpired(Instant now, Duration window) {
      return !now.isBefore(start.plus(window));
    }

    Duration timeUntilReset(Instant now, Duration window) {
      return Duration.between(now, start.plus(window));
    }
  }
}
