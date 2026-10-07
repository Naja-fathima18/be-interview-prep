package com.example.beinterviewprep.booking.service;

import java.time.LocalDateTime;

public record AvailableSlot(LocalDateTime startTime, LocalDateTime endTime) {}
