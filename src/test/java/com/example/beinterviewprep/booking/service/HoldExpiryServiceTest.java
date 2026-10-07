package com.example.beinterviewprep.booking.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.beinterviewprep.booking.BookingIntegrationTestSupport;
import com.example.beinterviewprep.booking.domain.Booking;
import com.example.beinterviewprep.booking.domain.BookingStatus;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class HoldExpiryServiceTest extends BookingIntegrationTestSupport {

  @Autowired private BookingService bookingService;
  @Autowired private HoldExpiryService holdExpiryService;

  @Test
  void sweepExpiresOnlyHoldsOlderThanFiveMinutes() {
    Booking stale = bookingService.hold(DOCTOR_ID, TEN_AM_TOMORROW, 1L);
    clock.advance(Duration.ofMinutes(3));
    Booking fresh = bookingService.hold(DOCTOR_ID, TEN_AM_TOMORROW.plusMinutes(30), 2L);
    clock.advance(Duration.ofMinutes(2));

    assertThat(holdExpiryService.expireStaleHolds()).isEqualTo(1);

    assertThat(statusOf(stale)).isEqualTo(BookingStatus.EXPIRED);
    assertThat(statusOf(fresh)).isEqualTo(BookingStatus.HELD);
    assertThat(bookingRepository.countActiveForSlot(DOCTOR_ID, TEN_AM_TOMORROW)).isZero();
  }

  @Test
  void sweepLeavesConfirmedBookingsAlone() {
    Booking booking = bookingService.hold(DOCTOR_ID, TEN_AM_TOMORROW, 1L);
    bookingService.confirm(booking.getId(), 1L);
    clock.advance(Duration.ofHours(1));

    assertThat(holdExpiryService.expireStaleHolds()).isZero();

    assertThat(statusOf(booking)).isEqualTo(BookingStatus.CONFIRMED);
  }

  private BookingStatus statusOf(Booking booking) {
    return bookingRepository.findById(booking.getId()).orElseThrow().getStatus();
  }
}
