package com.example.beinterviewprep.booking.domain;

import com.example.beinterviewprep.common.error.ConflictException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "booking")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Booking {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "doctor_id", nullable = false)
  private Long doctorId;

  @Column(name = "patient_id", nullable = false)
  private Long patientId;

  @Column(name = "start_time", nullable = false)
  private LocalDateTime startTime;

  @Column(name = "end_time", nullable = false)
  private LocalDateTime endTime;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private BookingStatus status;

  @Column(name = "hold_expires_at", nullable = false)
  private Instant holdExpiresAt;

  @Column(name = "confirmed_at")
  private Instant confirmedAt;

  @Column(name = "cancelled_at")
  private Instant cancelledAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Version
  @Getter(AccessLevel.NONE)
  private long version;

  private Booking(
      Long doctorId,
      Long patientId,
      LocalDateTime startTime,
      LocalDateTime endTime,
      Instant holdExpiresAt,
      Instant now) {
    this.doctorId = doctorId;
    this.patientId = patientId;
    this.startTime = startTime;
    this.endTime = endTime;
    this.status = BookingStatus.HELD;
    this.holdExpiresAt = holdExpiresAt;
    this.createdAt = now;
  }

  public static Booking hold(
      Long doctorId,
      Long patientId,
      LocalDateTime startTime,
      LocalDateTime endTime,
      Instant holdExpiresAt,
      Instant now) {
    return new Booking(doctorId, patientId, startTime, endTime, holdExpiresAt, now);
  }

  public boolean belongsTo(Long patientId) {
    return this.patientId.equals(patientId);
  }

  public boolean occupiesSlotAt(Instant now) {
    return status == BookingStatus.CONFIRMED
        || (status == BookingStatus.HELD && !isHoldExpiredAt(now));
  }

  public boolean isHoldExpiredAt(Instant now) {
    return !now.isBefore(holdExpiresAt);
  }

  public void confirm(Instant now) {
    if (status != BookingStatus.HELD) {
      throw new ConflictException("Booking " + id + " is " + status + " and cannot be confirmed");
    }
    if (isHoldExpiredAt(now)) {
      throw new ConflictException("Hold on booking " + id + " has expired");
    }
    status = BookingStatus.CONFIRMED;
    confirmedAt = now;
  }

  public void cancel(Instant now) {
    if (status != BookingStatus.CONFIRMED) {
      throw new ConflictException("Booking " + id + " is " + status + " and cannot be cancelled");
    }
    status = BookingStatus.CANCELLED;
    cancelledAt = now;
  }
}
