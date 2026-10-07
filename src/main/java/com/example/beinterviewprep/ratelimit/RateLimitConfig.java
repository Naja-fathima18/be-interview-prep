package com.example.beinterviewprep.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class RateLimitConfig {

  @Bean
  SlidingWindowRateLimiter slidingWindowRateLimiter(RateLimitProperties properties) {
    return new SlidingWindowRateLimiter(properties.limit(), properties.window(), Clock.systemUTC());
  }

  @Bean
  RateLimitFilter rateLimitFilter(
      SlidingWindowRateLimiter limiter, RateLimitProperties properties, ObjectMapper objectMapper) {
    return new RateLimitFilter(limiter, properties, objectMapper);
  }
}
