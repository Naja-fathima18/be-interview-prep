package com.example.beinterviewprep.booking.api;

import com.example.beinterviewprep.booking.service.AvailableSlot;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DoctorSlotsResponse(Long doctorId, LocalDate date, List<SlotResponse> slots) {

  public record SlotResponse(LocalDateTime startTime, LocalDateTime endTime) {}

  static DoctorSlotsResponse of(Long doctorId, LocalDate date, List<AvailableSlot> slots) {
    return new DoctorSlotsResponse(
        doctorId,
        date,
        slots.stream().map(slot -> new SlotResponse(slot.startTime(), slot.endTime())).toList());
  }
}
