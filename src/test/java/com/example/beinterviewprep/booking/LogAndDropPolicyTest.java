package com.example.beinterviewprep.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class LogAndDropPolicyTest {

  @Test
  void dropsTasksWithoutThrowingOrRunningThemOnTheCallerWhenSaturated() throws Exception {
    CountDownLatch release = new CountDownLatch(1);
    ThreadPoolExecutor executor =
        new ThreadPoolExecutor(
            1, 1, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1), new LogAndDropPolicy());
    AtomicBoolean droppedTaskRan = new AtomicBoolean();
    try {
      executor.execute(() -> awaitQuietly(release));
      executor.execute(() -> {});

      assertThatCode(() -> executor.execute(() -> droppedTaskRan.set(true)))
          .doesNotThrowAnyException();
    } finally {
      release.countDown();
      executor.shutdown();
      assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }
    assertThat(droppedTaskRan).isFalse();
  }

  private static void awaitQuietly(CountDownLatch latch) {
    try {
      latch.await(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
