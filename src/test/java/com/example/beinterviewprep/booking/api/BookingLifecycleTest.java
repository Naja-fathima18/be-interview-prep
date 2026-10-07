package com.example.beinterviewprep.booking.api;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.beinterviewprep.booking.BookingIntegrationTestSupport;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

class BookingLifecycleTest extends BookingIntegrationTestSupport {

  private static final String TEN_AM = "2030-01-15T10:00:00";
  private static final long PATIENT = 7L;
  private static final long OTHER_PATIENT = 8L;

  @Autowired private MockMvc mockMvc;

  @Test
  void listsAllFutureSlotsOfTheDoctorsWorkingDay() throws Exception {
    mockMvc
        .perform(get("/api/doctors/{id}/slots", DOCTOR_ID).param("date", "2030-01-15"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.doctorId").value(DOCTOR_ID))
        .andExpect(jsonPath("$.slots.length()").value(16))
        .andExpect(jsonPath("$.slots[0].startTime").value("2030-01-15T09:00:00"))
        .andExpect(jsonPath("$.slots[0].endTime").value("2030-01-15T09:30:00"));
  }

  @Test
  void holdReturnsCreatedBookingThatExpiresInFiveMinutes() throws Exception {
    hold(TEN_AM_TOMORROW, PATIENT)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("HELD"))
        .andExpect(jsonPath("$.startTime").value(TEN_AM))
        .andExpect(jsonPath("$.endTime").value("2030-01-15T10:30:00"))
        .andExpect(jsonPath("$.holdExpiresAt").value("2030-01-14T08:05:00Z"));
  }

  @Test
  void heldSlotIsNoLongerListedAndCannotBeHeldByAnotherPatient() throws Exception {
    holdId(TEN_AM_TOMORROW, PATIENT);

    expectSlotListed(false);
    hold(TEN_AM_TOMORROW, OTHER_PATIENT).andExpect(status().isConflict());
  }

  @Test
  void expiredHoldMakesSlotAvailableAgainForAnotherPatient() throws Exception {
    long expiredHold = holdId(TEN_AM_TOMORROW, PATIENT);

    clock.advance(Duration.ofMinutes(5));

    expectSlotListed(true);
    hold(TEN_AM_TOMORROW, OTHER_PATIENT)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.patientId").value(OTHER_PATIENT));
    expectSlotListed(false);
    confirm(expiredHold, PATIENT).andExpect(status().isConflict());
  }

  @Test
  void confirmBeforeExpiryBooksTheSlot() throws Exception {
    long bookingId = holdId(TEN_AM_TOMORROW, PATIENT);
    clock.advance(Duration.ofMinutes(4).plusSeconds(59));

    confirm(bookingId, PATIENT)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CONFIRMED"));

    clock.advance(Duration.ofHours(1));
    expectSlotListed(false);
    hold(TEN_AM_TOMORROW, OTHER_PATIENT).andExpect(status().isConflict());
  }

  @Test
  void confirmAfterHoldExpiredIsRejected() throws Exception {
    long bookingId = holdId(TEN_AM_TOMORROW, PATIENT);
    clock.advance(Duration.ofMinutes(5).plusSeconds(1));

    confirm(bookingId, PATIENT)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.detail").value("Hold on booking " + bookingId + " has expired"));
  }

  @Test
  void confirmByAnotherPatientIsNotFound() throws Exception {
    long bookingId = holdId(TEN_AM_TOMORROW, PATIENT);

    confirm(bookingId, OTHER_PATIENT).andExpect(status().isNotFound());
  }

  @Test
  void cancellingAConfirmedBookingFreesTheSlot() throws Exception {
    long bookingId = holdId(TEN_AM_TOMORROW, PATIENT);
    confirm(bookingId, PATIENT).andExpect(status().isOk());

    cancel(bookingId, PATIENT)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));

    expectSlotListed(true);
    hold(TEN_AM_TOMORROW, OTHER_PATIENT).andExpect(status().isCreated());
  }

  @Test
  void cancellingAnUnconfirmedHoldIsRejected() throws Exception {
    long bookingId = holdId(TEN_AM_TOMORROW, PATIENT);

    cancel(bookingId, PATIENT).andExpect(status().isConflict());
  }

  @Test
  void cancelByAnotherPatientIsNotFound() throws Exception {
    long bookingId = holdId(TEN_AM_TOMORROW, PATIENT);
    confirm(bookingId, PATIENT).andExpect(status().isOk());

    cancel(bookingId, OTHER_PATIENT).andExpect(status().isNotFound());
  }

  @Test
  void holdOffTheSlotGridIsBadRequest() throws Exception {
    hold(TOMORROW.atTime(10, 15), PATIENT).andExpect(status().isBadRequest());
    hold(TOMORROW.atTime(17, 0), PATIENT).andExpect(status().isBadRequest());
  }

  @Test
  void holdInThePastIsBadRequest() throws Exception {
    hold(TOMORROW.minusDays(1).atTime(7, 30), PATIENT).andExpect(status().isBadRequest());
  }

  @Test
  void slotsAlreadyPastInTheClinicTimeZoneAreHiddenAndRejected() throws Exception {
    clock.setInstant(Instant.parse("2030-01-15T04:00:00Z"));

    mockMvc
        .perform(get("/api/doctors/{id}/slots", DOCTOR_ID).param("date", "2030-01-15"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.slots[0].startTime").value(TEN_AM))
        .andExpect(jsonPath("$.slots[*].startTime", not(hasItem("2030-01-15T09:30:00"))));
    hold(TOMORROW.atTime(9, 0), PATIENT).andExpect(status().isBadRequest());
    hold(TOMORROW.atTime(9, 30), PATIENT).andExpect(status().isBadRequest());
    hold(TEN_AM_TOMORROW, PATIENT).andExpect(status().isCreated());
  }

  @Test
  void holdForUnknownDoctorIsNotFound() throws Exception {
    mockMvc
        .perform(
            post("/api/bookings/holds")
                .contentType(MediaType.APPLICATION_JSON)
                .content(holdJson(999L, TEN_AM_TOMORROW, PATIENT)))
        .andExpect(status().isNotFound());
  }

  @Test
  void holdWithoutPatientIsValidationError() throws Exception {
    mockMvc
        .perform(
            post("/api/bookings/holds")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"doctorId\":1,\"startTime\":\"" + TEN_AM + "\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.patientId").exists());
  }

  private void expectSlotListed(boolean listed) throws Exception {
    mockMvc
        .perform(get("/api/doctors/{id}/slots", DOCTOR_ID).param("date", "2030-01-15"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.slots[*].startTime", listed ? hasItem(TEN_AM) : not(hasItem(TEN_AM))));
  }

  private long holdId(LocalDateTime startTime, long patientId) throws Exception {
    String body =
        hold(startTime, patientId)
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return ((Number) JsonPath.read(body, "$.id")).longValue();
  }

  private ResultActions hold(LocalDateTime startTime, long patientId) throws Exception {
    return mockMvc.perform(
        post("/api/bookings/holds")
            .contentType(MediaType.APPLICATION_JSON)
            .content(holdJson(DOCTOR_ID, startTime, patientId)));
  }

  private ResultActions confirm(long bookingId, long patientId) throws Exception {
    return mockMvc.perform(
        post("/api/bookings/{id}/confirm", bookingId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(patientJson(patientId)));
  }

  private ResultActions cancel(long bookingId, long patientId) throws Exception {
    return mockMvc.perform(
        post("/api/bookings/{id}/cancel", bookingId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(patientJson(patientId)));
  }

  private static String holdJson(long doctorId, LocalDateTime startTime, long patientId) {
    return "{\"doctorId\":%d,\"startTime\":\"%s\",\"patientId\":%d}"
        .formatted(doctorId, startTime, patientId);
  }

  private static String patientJson(long patientId) {
    return "{\"patientId\":%d}".formatted(patientId);
  }
}
