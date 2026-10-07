package com.example.beinterviewprep.booking.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;

record ClinicTime(Instant instant, LocalDateTime local) {

  static ClinicTime now(Clock clock) {
    Instant instant = clock.instant();
    return new ClinicTime(instant, LocalDateTime.ofInstant(instant, clock.getZone()));
  }
}
