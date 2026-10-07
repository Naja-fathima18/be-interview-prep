package com.example.beinterviewprep.booking;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
@EnableScheduling
public class BookingConfiguration {

  public static final String NOTIFICATION_EXECUTOR = "bookingNotificationExecutor";
  public static final String CLOCK = "bookingClock";

  @Bean(name = CLOCK)
  Clock bookingClock(BookingProperties properties) {
    return Clock.system(properties.clinicZone());
  }

  @Bean(name = NOTIFICATION_EXECUTOR)
  ThreadPoolTaskExecutor bookingNotificationExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(500);
    executor.setThreadNamePrefix("booking-notify-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(10);
    return executor;
  }
}
