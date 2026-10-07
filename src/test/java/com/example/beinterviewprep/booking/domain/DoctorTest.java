package com.example.beinterviewprep.booking.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class DoctorTest {

  private static final Duration HALF_HOUR = Duration.ofMinutes(30);
  private static final LocalDate DAY = LocalDate.of(2030, 1, 15);

  private final Doctor doctor =
      new Doctor("Dr. Test", "General", LocalTime.of(9, 0), LocalTime.of(11, 0));

  @Test
  void splitsWorkingHoursIntoHalfHourSlots() {
    assertThat(doctor.slotStartsOn(DAY, HALF_HOUR))
        .containsExactly(
            DAY.atTime(9, 0), DAY.atTime(9, 30), DAY.atTime(10, 0), DAY.atTime(10, 30));
  }

  @Test
  void dropsTrailingSlotThatWouldRunPastEndOfDay() {
    Doctor shortDay = new Doctor("Dr. Short", "General", LocalTime.of(9, 0), LocalTime.of(9, 45));

    assertThat(shortDay.slotStartsOn(DAY, HALF_HOUR)).containsExactly(DAY.atTime(9, 0));
  }

  @Test
  void offersOnlyStartTimesAlignedToTheSlotGrid() {
    assertThat(doctor.offersSlotAt(DAY.atTime(10, 30), HALF_HOUR)).isTrue();
    assertThat(doctor.offersSlotAt(DAY.atTime(10, 15), HALF_HOUR)).isFalse();
    assertThat(doctor.offersSlotAt(DAY.atTime(11, 0), HALF_HOUR)).isFalse();
    assertThat(doctor.offersSlotAt(LocalDateTime.of(DAY, LocalTime.of(8, 30)), HALF_HOUR))
        .isFalse();
  }
}
