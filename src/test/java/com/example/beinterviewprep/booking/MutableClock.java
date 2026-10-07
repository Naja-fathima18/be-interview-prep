package com.example.beinterviewprep.booking;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

public class MutableClock extends Clock {

  private final AtomicReference<Instant> now;
  private final ZoneId zone;

  public MutableClock(Instant start, ZoneId zone) {
    this.now = new AtomicReference<>(start);
    this.zone = zone;
  }

  public void setInstant(Instant instant) {
    now.set(instant);
  }

  public void advance(Duration duration) {
    now.updateAndGet(current -> current.plus(duration));
  }

  @Override
  public ZoneId getZone() {
    return zone;
  }

  @Override
  public Clock withZone(ZoneId zone) {
    return new MutableClock(now.get(), zone);
  }

  @Override
  public Instant instant() {
    return now.get();
  }
}
