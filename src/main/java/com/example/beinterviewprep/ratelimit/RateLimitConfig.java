package com.example.beinterviewprep.ratelimit;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class RateLimitConfig {

  @Bean
  FixedWindowRateLimiter fixedWindowRateLimiter(RateLimitProperties properties) {
    return new FixedWindowRateLimiter(properties.limit(), properties.window(), Clock.systemUTC());
  }
}
