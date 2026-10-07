package com.example.beinterviewprep.booking;

import com.example.beinterviewprep.booking.persistence.BookingRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@AutoConfigureMockMvc
@SpringBootTest(properties = "booking.expiry-sweep-interval=1h")
@Import(BookingIntegrationTestSupport.ClockConfiguration.class)
public abstract class BookingIntegrationTestSupport {

  protected static final Instant NOW = Instant.parse("2030-01-14T08:00:00Z");
  protected static final LocalDate TOMORROW = LocalDate.of(2030, 1, 15);
  protected static final long DOCTOR_ID = 1L;
  protected static final LocalDateTime TEN_AM_TOMORROW = TOMORROW.atTime(10, 0);

  @Autowired protected MutableClock clock;
  @Autowired protected BookingRepository bookingRepository;

  @BeforeEach
  void resetBookingState() {
    bookingRepository.deleteAllInBatch();
    clock.setInstant(NOW);
  }

  @TestConfiguration
  static class ClockConfiguration {

    @Bean
    @Primary
    MutableClock mutableClock() {
      return new MutableClock(NOW, ZoneOffset.UTC);
    }
  }
}
