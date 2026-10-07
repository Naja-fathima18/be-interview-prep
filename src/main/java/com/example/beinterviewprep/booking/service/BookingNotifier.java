package com.example.beinterviewprep.booking.service;

import com.example.beinterviewprep.booking.domain.BookingConfirmedEvent;

public interface BookingNotifier {

  void bookingConfirmed(BookingConfirmedEvent event);
}
