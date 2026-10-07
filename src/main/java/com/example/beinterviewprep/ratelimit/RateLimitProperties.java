package com.example.beinterviewprep.ratelimit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import java.util.List;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "rate-limit")
public record RateLimitProperties(
    @DefaultValue("10") @Positive int limit,
    @DefaultValue("1m") @NotNull @DurationMin(seconds = 1) Duration window,
    @DefaultValue("X-API-Key") @NotBlank String header,
    @DefaultValue("/api/quotes/**") @NotEmpty List<@NotBlank String> paths) {}
