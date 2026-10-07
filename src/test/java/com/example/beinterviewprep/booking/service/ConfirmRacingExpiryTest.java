package com.example.beinterviewprep.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.verify;

import com.example.beinterviewprep.booking.BookingIntegrationTestSupport;
import com.example.beinterviewprep.booking.domain.Booking;
import com.example.beinterviewprep.booking.domain.BookingStatus;
import com.example.beinterviewprep.common.error.ConflictException;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

class ConfirmRacingExpiryTest extends BookingIntegrationTestSupport {

  private static final long PATIENT = 5L;

  @Autowired private BookingService bookingService;
  @Autowired private HoldExpiryService holdExpiryService;
  @Autowired private PlatformTransactionManager transactionManager;

  @Test
  void confirmIsRejectedWhenTheHoldWasExpiredByAnotherTransactionAfterBeingLoaded() {
    Booking hold = bookingService.hold(DOCTOR_ID, TEN_AM_TOMORROW, PATIENT);
    TransactionTemplate outer = new TransactionTemplate(transactionManager);

    assertThatThrownBy(
            () ->
                outer.executeWithoutResult(
                    status -> {
                      bookingRepository.findById(hold.getId()).orElseThrow();
                      expireAllHoldsInSeparateTransaction();
                      bookingService.confirm(hold.getId(), PATIENT);
                    }))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("changed concurrently");

    assertThat(bookingRepository.findById(hold.getId()).orElseThrow().getStatus())
        .isEqualTo(BookingStatus.EXPIRED);
    verify(bookingNotifier, after(300).never()).bookingConfirmed(any());
  }

  private void expireAllHoldsInSeparateTransaction() {
    TransactionTemplate requiresNew = new TransactionTemplate(transactionManager);
    requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    clock.advance(Duration.ofMinutes(6));
    requiresNew.executeWithoutResult(status -> holdExpiryService.expireStaleHolds());
    clock.setInstant(NOW);
  }
}
