package com.example.beinterviewprep.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class FixedWindowRateLimiterTest {

  private static final int LIMIT = 10;
  private static final Duration WINDOW = Duration.ofMinutes(1);

  private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
  private final FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(LIMIT, WINDOW, clock);

  @Test
  void allowsRequestsUpToLimitAndRejectsTheNext() {
    for (int i = 1; i <= LIMIT; i++) {
      RateLimitDecision decision = limiter.tryAcquire("key");
      assertThat(decision.allowed()).isTrue();
      assertThat(decision.remaining()).isEqualTo(LIMIT - i);
    }

    RateLimitDecision rejected = limiter.tryAcquire("key");

    assertThat(rejected.allowed()).isFalse();
    assertThat(rejected.remaining()).isZero();
    assertThat(rejected.retryAfterSeconds()).isEqualTo(60);
  }

  @Test
  void retryAfterCountsDownToWindowResetRoundedUp() {
    exhaust("key");
    clock.advance(Duration.ofMillis(45_500));

    assertThat(limiter.tryAcquire("key").retryAfterSeconds()).isEqualTo(15);
  }

  @Test
  void retryAfterIsAtLeastOneSecond() {
    exhaust("key");
    clock.advance(Duration.ofMillis(59_999));

    assertThat(limiter.tryAcquire("key").retryAfterSeconds()).isEqualTo(1);
  }

  @Test
  void allowsRequestsAgainOnceWindowHasPassed() {
    exhaust("key");
    assertThat(limiter.tryAcquire("key").allowed()).isFalse();

    clock.advance(WINDOW);

    RateLimitDecision decision = limiter.tryAcquire("key");
    assertThat(decision.allowed()).isTrue();
    assertThat(decision.remaining()).isEqualTo(LIMIT - 1);
  }

  @Test
  void countsEachKeyIndependently() {
    exhaust("alice");

    assertThat(limiter.tryAcquire("alice").allowed()).isFalse();
    assertThat(limiter.tryAcquire("bob").allowed()).isTrue();
  }

  @Test
  void evictsExpiredKeysAfterWindowPasses() {
    limiter.tryAcquire("alice");
    limiter.tryAcquire("bob");
    clock.advance(WINDOW);

    limiter.tryAcquire("carol");

    assertThat(limiter.trackedKeys()).isEqualTo(1);
  }

  @Test
  void allowsExactlyLimitRequestsWhenManyArriveAtTheSameMoment() throws Exception {
    int threads = 50;
    ExecutorService executor = Executors.newFixedThreadPool(threads);
    CountDownLatch ready = new CountDownLatch(threads);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<Boolean>> results = new ArrayList<>();
    try {
      for (int i = 0; i < threads; i++) {
        results.add(
            executor.submit(
                () -> {
                  ready.countDown();
                  start.await();
                  return limiter.tryAcquire("burst").allowed();
                }));
      }
      ready.await(5, TimeUnit.SECONDS);
      start.countDown();

      long allowed = 0;
      for (Future<Boolean> result : results) {
        if (result.get(5, TimeUnit.SECONDS)) {
          allowed++;
        }
      }
      assertThat(allowed).isEqualTo(LIMIT);
    } finally {
      executor.shutdownNow();
    }
  }

  private void exhaust(String key) {
    for (int i = 0; i < LIMIT; i++) {
      limiter.tryAcquire(key);
    }
  }
}
