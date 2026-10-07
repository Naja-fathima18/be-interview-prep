package com.example.beinterviewprep.booking;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "booking")
public record BookingProperties(
    @NotNull Duration holdDuration,
    @NotNull Duration slotLength,
    @NotNull Duration expirySweepInterval) {}
