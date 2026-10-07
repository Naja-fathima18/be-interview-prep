package com.example.beinterviewprep.booking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class HoldExpiryJob {

  private final HoldExpiryService holdExpiryService;

  @Scheduled(
      initialDelayString = "${booking.expiry-sweep-interval}",
      fixedDelayString = "${booking.expiry-sweep-interval}")
  void expireStaleHolds() {
    holdExpiryService.expireStaleHolds();
  }
}
