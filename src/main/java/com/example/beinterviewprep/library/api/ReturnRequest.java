package com.example.beinterviewprep.library.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReturnRequest(@NotNull @Positive Long memberId) {}
