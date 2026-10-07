package com.example.beinterviewprep.booking.api;

import com.example.beinterviewprep.booking.domain.Booking;
import com.example.beinterviewprep.booking.domain.BookingStatus;
import java.time.Instant;
import java.time.LocalDateTime;

public record BookingResponse(
    Long id,
    Long doctorId,
    Long patientId,
    LocalDateTime startTime,
    LocalDateTime endTime,
    BookingStatus status,
    Instant holdExpiresAt) {

  static BookingResponse from(Booking booking) {
    return new BookingResponse(
        booking.getId(),
        booking.getDoctorId(),
        booking.getPatientId(),
        booking.getStartTime(),
        booking.getEndTime(),
        booking.getStatus(),
        booking.getHoldExpiresAt());
  }
}
