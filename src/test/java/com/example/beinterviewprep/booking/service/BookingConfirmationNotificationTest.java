package com.example.beinterviewprep.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.example.beinterviewprep.booking.BookingIntegrationTestSupport;
import com.example.beinterviewprep.booking.domain.Booking;
import com.example.beinterviewprep.booking.domain.BookingConfirmedEvent;
import com.example.beinterviewprep.booking.domain.BookingStatus;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

class BookingConfirmationNotificationTest extends BookingIntegrationTestSupport {

  private static final long PATIENT = 3L;

  @Autowired private BookingService bookingService;
  @Autowired private TransactionTemplate transactionTemplate;

  @Test
  void notifiesOnABackgroundThreadAfterTheConfirmationCommits() throws Exception {
    Booking hold = bookingService.hold(DOCTOR_ID, TEN_AM_TOMORROW, PATIENT);
    CompletableFuture<String> notifiedOn = new CompletableFuture<>();
    doAnswer(
            invocation -> {
              notifiedOn.complete(Thread.currentThread().getName());
              return null;
            })
        .when(bookingNotifier)
        .bookingConfirmed(any());

    bookingService.confirm(hold.getId(), PATIENT);

    assertThat(notifiedOn.get(5, TimeUnit.SECONDS)).startsWith("booking-notify-");
    verify(bookingNotifier)
        .bookingConfirmed(
            new BookingConfirmedEvent(hold.getId(), DOCTOR_ID, PATIENT, TEN_AM_TOMORROW));
  }

  @Test
  void slowNotificationDoesNotDelayTheConfirmation() throws Exception {
    Booking hold = bookingService.hold(DOCTOR_ID, TEN_AM_TOMORROW, PATIENT);
    CountDownLatch releaseNotifier = new CountDownLatch(1);
    doAnswer(
            invocation -> {
              releaseNotifier.await(10, TimeUnit.SECONDS);
              return null;
            })
        .when(bookingNotifier)
        .bookingConfirmed(any());

    try {
      Booking confirmed = bookingService.confirm(hold.getId(), PATIENT);

      assertThat(confirmed.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    } finally {
      releaseNotifier.countDown();
    }
    verify(bookingNotifier, timeout(5000)).bookingConfirmed(any());
  }

  @Test
  void doesNotNotifyWhenTheConfirmationRollsBack() {
    Booking hold = bookingService.hold(DOCTOR_ID, TEN_AM_TOMORROW, PATIENT);

    transactionTemplate.executeWithoutResult(
        status -> {
          bookingService.confirm(hold.getId(), PATIENT);
          status.setRollbackOnly();
        });

    verify(bookingNotifier, after(500).never()).bookingConfirmed(any());
    assertThat(bookingRepository.findById(hold.getId()).orElseThrow().getStatus())
        .isEqualTo(BookingStatus.HELD);
  }

  @Test
  void doesNotNotifyWhenConfirmationIsRejected() {
    Booking hold = bookingService.hold(DOCTOR_ID, TEN_AM_TOMORROW, PATIENT);
    clock.advance(Duration.ofMinutes(6));

    assertThat(catchThrowable(() -> bookingService.confirm(hold.getId(), PATIENT))).isNotNull();

    verify(bookingNotifier, after(500).never()).bookingConfirmed(any());
  }
}
