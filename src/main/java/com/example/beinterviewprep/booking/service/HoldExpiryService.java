package com.example.beinterviewprep.booking.service;

import com.example.beinterviewprep.booking.domain.BookingStatus;
import com.example.beinterviewprep.booking.persistence.BookingRepository;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HoldExpiryService {

  private final BookingRepository bookings;
  private final Clock clock;

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
