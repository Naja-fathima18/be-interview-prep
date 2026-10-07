package com.example.beinterviewprep.booking.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.beinterviewprep.booking.BookingIntegrationTestSupport;
import com.example.beinterviewprep.booking.domain.Booking;
import com.example.beinterviewprep.common.error.ConflictException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ConcurrentHoldTest extends BookingIntegrationTestSupport {

  private static final int PATIENTS = 20;

  @Autowired private BookingService bookingService;

  @Test
  void onlyOneOfTwentySimultaneousPatientsHoldsTheSameSlot() throws Exception {
    List<Outcome> outcomes = holdSameSlotConcurrently(PATIENTS);

    assertThat(outcomes).filteredOn(Outcome::held).hasSize(1);
    assertThat(outcomes)
        .filteredOn(outcome -> !outcome.held())
        .hasSize(PATIENTS - 1)
        .allSatisfy(outcome -> assertThat(outcome.failure()).isInstanceOf(ConflictException.class));
    assertThat(activeBookingsForSlot(TEN_AM_TOMORROW)).isEqualTo(1);
  }

  @Test
  void onlyOneOfTwentySimultaneousPatientsClaimsASlotWhoseHoldHasExpired() throws Exception {
    bookingService.hold(DOCTOR_ID, TEN_AM_TOMORROW, 999L);
    clock.advance(Duration.ofMinutes(6));

    List<Outcome> outcomes = holdSameSlotConcurrently(PATIENTS);

    assertThat(outcomes).filteredOn(Outcome::held).hasSize(1);
    assertThat(activeBookingsForSlot(TEN_AM_TOMORROW)).isEqualTo(1);
  }

  private List<Outcome> holdSameSlotConcurrently(int patients) throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(patients);
    CountDownLatch ready = new CountDownLatch(patients);
    CountDownLatch start = new CountDownLatch(1);
    try {
      List<Future<Booking>> futures = new ArrayList<>();
      for (long patientId = 1; patientId <= patients; patientId++) {
        long patient = patientId;
        futures.add(
            pool.submit(
                () -> {
                  ready.countDown();
                  start.await();
                  return bookingService.hold(DOCTOR_ID, TEN_AM_TOMORROW, patient);
                }));
      }
      assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      List<Outcome> outcomes = new ArrayList<>();
      for (Future<Booking> future : futures) {
        outcomes.add(outcomeOf(future));
      }
      return outcomes;
    } finally {
      pool.shutdownNow();
    }
  }

  private Outcome outcomeOf(Future<Booking> future) throws Exception {
    try {
      future.get(60, TimeUnit.SECONDS);
      return new Outcome(true, null);
    } catch (ExecutionException e) {
      return new Outcome(false, e.getCause());
    }
  }

  private record Outcome(boolean held, Throwable failure) {}
}
