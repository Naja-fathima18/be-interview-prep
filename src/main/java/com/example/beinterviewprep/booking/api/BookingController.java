package com.example.beinterviewprep.booking.api;

import com.example.beinterviewprep.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

  private final BookingService bookingService;

  @PostMapping("/holds")
  @ResponseStatus(HttpStatus.CREATED)
  BookingResponse hold(@Valid @RequestBody HoldRequest request) {
    return BookingResponse.from(
        bookingService.hold(request.doctorId(), request.startTime(), request.patientId()));
  }
}
