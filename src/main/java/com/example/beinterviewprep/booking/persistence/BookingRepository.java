package com.example.beinterviewprep.booking.persistence;

import com.example.beinterviewprep.booking.domain.Booking;
import com.example.beinterviewprep.booking.domain.BookingStatus;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {

  @Query(
      """
      select b from Booking b
      where b.doctorId = :doctorId
        and b.status in :activeStatuses
        and b.startTime >= :from and b.startTime < :to
      """)
  List<Booking> findActiveForDoctorBetween(
      @Param("doctorId") Long doctorId,
      @Param("from") LocalDateTime from,
      @Param("to") LocalDateTime to,
      @Param("activeStatuses") Collection<BookingStatus> activeStatuses);

  default List<Booking> findActiveForDoctorBetween(
      Long doctorId, LocalDateTime from, LocalDateTime to) {
    return findActiveForDoctorBetween(doctorId, from, to, BookingStatus.ACTIVE);
  }

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      """
      update Booking b
      set b.status = :expired, b.version = b.version + 1
      where b.status = :held and b.holdExpiresAt <= :now
        and b.doctorId = :doctorId and b.startTime = :startTime
      """)
  int expireStaleHoldForSlot(
      @Param("doctorId") Long doctorId,
      @Param("startTime") LocalDateTime startTime,
      @Param("now") Instant now,
      @Param("held") BookingStatus held,
      @Param("expired") BookingStatus expired);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      """
      update Booking b
      set b.status = :expired, b.version = b.version + 1
      where b.status = :held and b.holdExpiresAt <= :now
      """)
  int expireStaleHolds(
      @Param("now") Instant now,
      @Param("held") BookingStatus held,
      @Param("expired") BookingStatus expired);
}
