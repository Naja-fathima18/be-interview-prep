package com.example.beinterviewprep.booking.domain;

import java.util.Set;

public enum BookingStatus {
  HELD,
  CONFIRMED,
  EXPIRED,
  CANCELLED;

  public static final Set<BookingStatus> ACTIVE = Set.of(HELD, CONFIRMED);
}
