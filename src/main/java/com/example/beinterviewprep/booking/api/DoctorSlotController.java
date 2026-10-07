package com.example.beinterviewprep.booking.api;

import com.example.beinterviewprep.booking.service.SlotService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorSlotController {

  private final SlotService slotService;

  @GetMapping("/{doctorId}/slots")
  DoctorSlotsResponse availableSlots(
      @PathVariable Long doctorId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return DoctorSlotsResponse.of(doctorId, date, slotService.availableSlots(doctorId, date));
  }
}
