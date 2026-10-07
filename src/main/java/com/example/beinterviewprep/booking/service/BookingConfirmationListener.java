package com.example.beinterviewprep.booking.service;

import com.example.beinterviewprep.booking.BookingConfiguration;
import com.example.beinterviewprep.booking.domain.BookingConfirmedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
class BookingConfirmationListener {

  private final BookingNotifier notifier;

  @Async(BookingConfiguration.NOTIFICATION_EXECUTOR)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  void onBookingConfirmed(BookingConfirmedEvent event) {
    try {
      notifier.bookingConfirmed(event);
    } catch (RuntimeException e) {
      log.error("Failed to send confirmation for booking {}", event.bookingId(), e);
    }
  }
}
