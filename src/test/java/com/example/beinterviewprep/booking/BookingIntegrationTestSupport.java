package com.example.beinterviewprep.booking;

import com.example.beinterviewprep.booking.persistence.BookingRepository;
import com.example.beinterviewprep.booking.service.BookingNotifier;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@AutoConfigureMockMvc
@SpringBootTest(properties = "booking.expiry-sweep-interval=1h")
public abstract class BookingIntegrationTestSupport {

  protected static final Instant NOW = Instant.parse("2030-01-14T08:00:00Z");
  protected static final LocalDate TOMORROW = LocalDate.of(2030, 1, 15);
  protected static final long DOCTOR_ID = 1L;
  protected static final LocalDateTime TEN_AM_TOMORROW = TOMORROW.atTime(10, 0);

  protected static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Kolkata");

  @TestBean(name = BookingConfiguration.CLOCK, methodName = "clinicClock")
  private Clock bookingClock;

  protected MutableClock clock;

  @Autowired protected BookingRepository bookingRepository;
  @MockitoBean protected BookingNotifier bookingNotifier;

  @BeforeEach
  void resetBookingState() {
    bookingRepository.deleteAllInBatch();
    clock = (MutableClock) bookingClock;
    clock.setInstant(NOW);
  }

  static Clock clinicClock() {
    return new MutableClock(NOW, CLINIC_ZONE);
  }
}
