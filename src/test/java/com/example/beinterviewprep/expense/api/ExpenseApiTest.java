package com.example.beinterviewprep.expense.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.beinterviewprep.expense.persistence.ExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class ExpenseApiTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ExpenseRepository repository;

  @BeforeEach
  void cleanDatabase() {
    repository.deleteAll();
  }

  @Test
  void summaryIncludesFirstAndLastDayOfMonthAndExcludesNeighbouringDays() throws Exception {
    create("100.00", "FOOD", "2024-01-31");
    create("0.10", "FOOD", "2024-02-01");
    create("0.20", "FOOD", "2024-02-29");
    create("50.5", "BILLS", "2024-02-29");
    create("200.00", "TRAVEL", "2024-03-01");

    mockMvc
        .perform(get("/api/expenses/summary").param("month", "2024-02"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.month").value("2024-02"))
        .andExpect(jsonPath("$.totalsByCategory.FOOD").value(0.30))
        .andExpect(jsonPath("$.totalsByCategory.BILLS").value(50.50))
        .andExpect(jsonPath("$.totalsByCategory.TRAVEL").value(0.00))
        .andExpect(jsonPath("$.totalsByCategory.OTHER").value(0.00))
        .andExpect(jsonPath("$.total").value(50.80));
  }

  @Test
  void summaryTotalsAreExactDecimals() throws Exception {
    create("0.10", "FOOD", "2026-02-10");
    create("0.20", "OTHER", "2026-02-11");

    String body =
        mockMvc
            .perform(get("/api/expenses/summary").param("month", "2026-02"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(body)
        .contains("\"FOOD\":0.10", "\"OTHER\":0.20", "\"TRAVEL\":0.00", "\"total\":0.30");
  }

  @Test
  void rejectsSummaryWithMalformedMonth() throws Exception {
    mockMvc
        .perform(get("/api/expenses/summary").param("month", "2026-13"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createsReadsUpdatesAndDeletesAnExpense() throws Exception {
    String location =
        mockMvc
            .perform(
                post("/api/expenses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json("12.5", "food", "2026-02-03", "lunch")))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", startsWith("http://localhost/api/expenses/")))
            .andExpect(jsonPath("$.amount").value(12.50))
            .andExpect(jsonPath("$.category").value("FOOD"))
            .andExpect(jsonPath("$.note").value("lunch"))
            .andReturn()
            .getResponse()
            .getHeader("Location");

    mockMvc
        .perform(get(location))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.date").value("2026-02-03"));

    mockMvc
        .perform(
            put(location)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("20.00", "TRAVEL", "2026-02-04", null)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.amount").value(20.00))
        .andExpect(jsonPath("$.category").value("TRAVEL"));

    mockMvc.perform(delete(location)).andExpect(status().isNoContent());
    mockMvc.perform(get(location)).andExpect(status().isNotFound());
  }

  @Test
  void returnsNotFoundWhenUpdatingUnknownExpense() throws Exception {
    mockMvc
        .perform(
            put("/api/expenses/999999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("1.00", "FOOD", "2026-02-04", null)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Resource not found"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "0.00", "-1.00", "1.005", "12345678901.00"})
  void rejectsInvalidAmounts(String amount) throws Exception {
    mockMvc
        .perform(
            post("/api/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(amount, "FOOD", "2026-02-03", null)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.amount").exists());
  }

  @Test
  void rejectsMissingRequiredFields() throws Exception {
    mockMvc
        .perform(post("/api/expenses").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.amount").exists())
        .andExpect(jsonPath("$.errors.category").exists())
        .andExpect(jsonPath("$.errors.date").exists());
  }

  @Test
  void rejectsUnknownCategory() throws Exception {
    mockMvc
        .perform(
            post("/api/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("1.00", "GROCERIES", "2026-02-03", null)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void filtersListByInclusiveDateRangeAndCategory() throws Exception {
    create("1.00", "TRAVEL", "2026-03-09");
    create("2.00", "TRAVEL", "2026-03-10");
    create("3.00", "TRAVEL", "2026-03-12");
    create("4.00", "FOOD", "2026-03-11");
    create("5.00", "TRAVEL", "2026-03-13");

    mockMvc
        .perform(
            get("/api/expenses")
                .param("from", "2026-03-10")
                .param("to", "2026-03-12")
                .param("category", "travel"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.content[0].date").value("2026-03-12"))
        .andExpect(jsonPath("$.content[1].date").value("2026-03-10"))
        .andExpect(jsonPath("$.totalElements").value(2));
  }

  @Test
  void rejectsListWhenFromIsAfterTo() throws Exception {
    mockMvc
        .perform(get("/api/expenses").param("from", "2026-03-12").param("to", "2026-03-10"))
        .andExpect(status().isBadRequest());
  }

  private ResultActions create(String amount, String category, String date) throws Exception {
    return mockMvc
        .perform(
            post("/api/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(amount, category, date, null)))
        .andExpect(status().isCreated());
  }

  private static String json(String amount, String category, String date, String note) {
    String noteJson = note == null ? "null" : "\"" + note + "\"";
    return """
        {"amount": %s, "category": "%s", "date": "%s", "note": %s}
        """
        .formatted(amount, category, date, noteJson);
  }
}
