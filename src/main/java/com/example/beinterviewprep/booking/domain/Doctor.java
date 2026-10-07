package com.example.beinterviewprep.booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "doctor")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Doctor {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String specialty;

  @Column(name = "work_start", nullable = false)
  private LocalTime workStart;

  @Column(name = "work_end", nullable = false)
  private LocalTime workEnd;

  public Doctor(String name, String specialty, LocalTime workStart, LocalTime workEnd) {
    this.name = name;
    this.specialty = specialty;
    this.workStart = workStart;
    this.workEnd = workEnd;
  }

  public List<LocalDateTime> slotStartsOn(LocalDate date, Duration slotLength) {
    List<LocalDateTime> starts = new ArrayList<>();
    LocalDateTime end = date.atTime(workEnd);
    for (LocalDateTime start = date.atTime(workStart);
        !start.plus(slotLength).isAfter(end);
        start = start.plus(slotLength)) {
      starts.add(start);
    }
    return starts;
  }

  public boolean offersSlotAt(LocalDateTime start, Duration slotLength) {
    return slotStartsOn(start.toLocalDate(), slotLength).contains(start);
  }
}
