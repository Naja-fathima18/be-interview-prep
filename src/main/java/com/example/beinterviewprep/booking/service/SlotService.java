package com.example.beinterviewprep.booking.service;

import com.example.beinterviewprep.booking.BookingProperties;
import com.example.beinterviewprep.booking.domain.Booking;
import com.example.beinterviewprep.booking.domain.Doctor;
import com.example.beinterviewprep.booking.persistence.BookingRepository;
import com.example.beinterviewprep.booking.persistence.DoctorRepository;
import com.example.beinterviewprep.common.error.NotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SlotService {

  private final DoctorRepository doctors;
  private final BookingRepository bookings;
  private final BookingProperties properties;
  private final Clock clock;

  @Transactional(readOnly = true)
  public List<AvailableSlot> availableSlots(Long doctorId, LocalDate date) {
    Doctor doctor = findDoctor(doctorId);
    Instant now = clock.instant();
    LocalDateTime localNow = LocalDateTime.now(clock);
    Set<LocalDateTime> occupied = occupiedStarts(doctorId, date, now);
    return doctor.slotStartsOn(date, properties.slotLength()).stream()
        .filter(start -> start.isAfter(localNow))
        .filter(start -> !occupied.contains(start))
        .map(start -> new AvailableSlot(start, start.plus(properties.slotLength())))
        .toList();
  }

  @Transactional(readOnly = true)
  public Doctor findDoctor(Long doctorId) {
    return doctors
        .findById(doctorId)
        .orElseThrow(() -> new NotFoundException("Doctor " + doctorId + " not found"));
  }

  private Set<LocalDateTime> occupiedStarts(Long doctorId, LocalDate date, Instant now) {
    return bookings
        .findActiveForDoctorBetween(doctorId, date.atStartOfDay(), date.plusDays(1).atStartOfDay())
        .stream()
        .filter(booking -> booking.occupiesSlotAt(now))
        .map(Booking::getStartTime)
        .collect(Collectors.toSet());
  }
}
