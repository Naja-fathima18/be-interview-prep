package com.example.beinterviewprep.library.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record BookRequest(
    @NotBlank @Size(max = 255) String title,
    @NotBlank @Size(max = 255) String author,
    @NotBlank
        @Pattern(
            regexp = "^([0-9]{9}[0-9X]|97[89][0-9]{10})$",
            message = "must be a 10 or 13 character ISBN without hyphens")
        String isbn,
    @NotNull @Positive @NotFutureYear Integer publishedYear) {}
