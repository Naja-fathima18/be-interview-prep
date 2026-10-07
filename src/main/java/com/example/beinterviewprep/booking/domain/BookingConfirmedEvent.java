package com.example.beinterviewprep.booking.domain;

import java.time.LocalDateTime;

public record BookingConfirmedEvent(
    Long bookingId, Long doctorId, Long patientId, LocalDateTime startTime) {

  public static BookingConfirmedEvent of(Booking booking) {
    return new BookingConfirmedEvent(
        booking.getId(), booking.getDoctorId(), booking.getPatientId(), booking.getStartTime());
  }
}
