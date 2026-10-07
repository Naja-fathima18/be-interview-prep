package com.example.beinterviewprep.booking.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;

public record HoldRequest(
    @NotNull @Positive Long doctorId,
    @NotNull LocalDateTime startTime,
    @NotNull @Positive Long patientId) {}
