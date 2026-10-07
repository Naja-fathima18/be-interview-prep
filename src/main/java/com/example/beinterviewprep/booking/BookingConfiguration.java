package com.example.beinterviewprep.booking;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class BookingConfiguration {

  @Bean
  Clock clock() {
    return Clock.systemDefaultZone();
  }
}
