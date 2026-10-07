package com.example.beinterviewprep.booking.service;

import com.example.beinterviewprep.booking.domain.BookingConfirmedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class LoggingBookingNotifier implements BookingNotifier {

  @Override
  public void bookingConfirmed(BookingConfirmedEvent event) {
    log.info(
        "Booking {} confirmed: notifying patient {} of appointment with doctor {} at {}",
        event.bookingId(),
        event.patientId(),
        event.doctorId(),
        event.startTime());
  }
}
