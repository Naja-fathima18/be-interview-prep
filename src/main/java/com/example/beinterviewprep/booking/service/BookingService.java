package com.example.beinterviewprep.booking.service;

import com.example.beinterviewprep.booking.BookingConfiguration;
import com.example.beinterviewprep.booking.BookingProperties;
import com.example.beinterviewprep.booking.domain.Booking;
import com.example.beinterviewprep.booking.domain.BookingConfirmedEvent;
import com.example.beinterviewprep.booking.domain.BookingStatus;
import com.example.beinterviewprep.booking.domain.Doctor;
import com.example.beinterviewprep.booking.persistence.BookingRepository;
import com.example.beinterviewprep.common.error.BadRequestException;
import com.example.beinterviewprep.common.error.ConflictException;
import com.example.beinterviewprep.common.error.NotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class BookingService {

  private final SlotService slotService;
  private final BookingRepository bookings;
  private final BookingProperties properties;
  private final Clock clock;
  private final ApplicationEventPublisher events;

  public BookingService(
      SlotService slotService,
      BookingRepository bookings,
      BookingProperties properties,
      @Qualifier(BookingConfiguration.CLOCK) Clock clock,
      ApplicationEventPublisher events) {
    this.slotService = slotService;
    this.bookings = bookings;
    this.properties = properties;
    this.clock = clock;
    this.events = events;
  }

  @Transactional
  public Booking hold(Long doctorId, LocalDateTime startTime, Long patientId) {
    Doctor doctor = slotService.findDoctor(doctorId);
    ClinicTime clinicNow = ClinicTime.now(clock);
    requireBookableSlot(doctor, startTime, clinicNow.local());
    Instant now = clinicNow.instant();
    if (bookings.isSlotOccupied(
        doctorId, startTime, now, BookingStatus.HELD, BookingStatus.CONFIRMED)) {
      throw slotTaken(doctorId, startTime);
    }
    bookings.expireStaleHoldForSlot(
        doctorId, startTime, now, BookingStatus.HELD, BookingStatus.EXPIRED);
    Booking hold =
        Booking.hold(
            doctorId,
            patientId,
            startTime,
            startTime.plus(properties.slotLength()),
            now.plus(properties.holdDuration()),
            now);
    try {
      Booking saved = bookings.saveAndFlush(hold);
      log.info("Booking {} held for doctor {} at {}", saved.getId(), doctorId, startTime);
      return saved;
    } catch (DataIntegrityViolationException | ConcurrencyFailureException e) {
      throw slotTaken(doctorId, startTime);
    }
  }

  @Transactional
  public Booking confirm(Long bookingId, Long patientId) {
    Booking booking = findOwnedBy(bookingId, patientId);
    booking.confirm(clock.instant());
    flushGuardingAgainstConcurrentChange(bookingId);
    events.publishEvent(BookingConfirmedEvent.of(booking));
    log.info("Booking {} confirmed", bookingId);
    return booking;
  }

  @Transactional
  public Booking cancel(Long bookingId, Long patientId) {
    Booking booking = findOwnedBy(bookingId, patientId);
    booking.cancel(clock.instant());
    flushGuardingAgainstConcurrentChange(bookingId);
    log.info("Booking {} cancelled", bookingId);
    return booking;
  }

  private Booking findOwnedBy(Long bookingId, Long patientId) {
    return bookings
        .findById(bookingId)
        .filter(booking -> booking.belongsTo(patientId))
        .orElseThrow(() -> new NotFoundException("Booking " + bookingId + " not found"));
  }

  private void flushGuardingAgainstConcurrentChange(Long bookingId) {
    try {
      bookings.flush();
    } catch (ConcurrencyFailureException e) {
      throw new ConflictException("Booking " + bookingId + " was changed concurrently");
    }
  }

  private void requireBookableSlot(Doctor doctor, LocalDateTime startTime, LocalDateTime localNow) {
    if (!doctor.offersSlotAt(startTime, properties.slotLength())) {
      throw new BadRequestException(
          "Doctor " + doctor.getId() + " has no slot starting at " + startTime);
    }
    if (!startTime.isAfter(localNow)) {
      throw new BadRequestException("Slot at " + startTime + " is in the past");
    }
  }

  private ConflictException slotTaken(Long doctorId, LocalDateTime startTime) {
    return new ConflictException(
        "Slot at " + startTime + " for doctor " + doctorId + " is no longer available");
  }
}
