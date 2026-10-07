package com.example.beinterviewprep.booking.service;

import com.example.beinterviewprep.booking.BookingConfiguration;
import com.example.beinterviewprep.booking.domain.BookingStatus;
import com.example.beinterviewprep.booking.persistence.BookingRepository;
import java.time.Clock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class HoldExpiryService {

  private final BookingRepository bookings;
  private final Clock clock;

  public HoldExpiryService(
      BookingRepository bookings, @Qualifier(BookingConfiguration.CLOCK) Clock clock) {
    this.bookings = bookings;
    this.clock = clock;
  }

  @Transactional
  public int expireStaleHolds() {
    int expired =
        bookings.expireStaleHolds(clock.instant(), BookingStatus.HELD, BookingStatus.EXPIRED);
    if (expired > 0) {
      log.info("Expired {} unconfirmed holds", expired);
    }
    return expired;
  }
}
