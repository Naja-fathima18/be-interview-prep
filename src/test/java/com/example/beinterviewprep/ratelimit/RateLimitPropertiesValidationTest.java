package com.example.beinterviewprep.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class RateLimitPropertiesValidationTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
          .withUserConfiguration(PropertiesConfig.class);

  @Test
  void usesDefaultsWhenNothingIsConfigured() {
    runner.run(
        context -> {
          RateLimitProperties properties = context.getBean(RateLimitProperties.class);
          assertThat(properties.limit()).isEqualTo(10);
          assertThat(properties.window()).hasMinutes(1);
          assertThat(properties.header()).isEqualTo("X-API-Key");
        });
  }

  @Test
  void failsStartupWhenLimitIsNotPositive() {
    runner.withPropertyValues("rate-limit.limit=0").run(context -> assertThat(context).hasFailed());
  }

  @Test
  void failsStartupWhenWindowIsShorterThanOneSecond() {
    runner
        .withPropertyValues("rate-limit.window=500ms")
        .run(context -> assertThat(context).hasFailed());
  }

  @Configuration
  @EnableConfigurationProperties(RateLimitProperties.class)
  static class PropertiesConfig {}
}
