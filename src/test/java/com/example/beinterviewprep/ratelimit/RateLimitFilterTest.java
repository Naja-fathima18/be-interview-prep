package com.example.beinterviewprep.ratelimit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.beinterviewprep.quote.api.QuoteController;
import com.example.beinterviewprep.quote.service.QuoteService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(QuoteController.class)
@Import({RateLimitConfig.class, QuoteService.class})
@EnableConfigurationProperties(RateLimitProperties.class)
class RateLimitFilterTest {

  private static final String RANDOM_QUOTE = "/api/quotes/random";

  @Autowired private MockMvc mockMvc;

  @Test
  void rejectsEleventhRequestInAMinuteWithRetryAfter() throws Exception {
    String apiKey = uniqueKey();
    for (int i = 1; i <= 10; i++) {
      mockMvc
          .perform(get(RANDOM_QUOTE).header("X-API-Key", apiKey))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.text").isNotEmpty())
          .andExpect(header().string(RateLimitFilter.LIMIT_HEADER, "10"))
          .andExpect(header().string(RateLimitFilter.REMAINING_HEADER, String.valueOf(10 - i)));
    }

    mockMvc
        .perform(get(RANDOM_QUOTE).header("X-API-Key", apiKey))
        .andExpect(status().isTooManyRequests())
        .andExpect(RetryAfterMatchers.retryAfterCloseTo(60))
        .andExpect(header().string(RateLimitFilter.REMAINING_HEADER, "0"))
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(429))
        .andExpect(jsonPath("$.title").value("Too many requests"))
        .andExpect(jsonPath("$.instance").value(RANDOM_QUOTE));
  }

  @Test
  void limitsEachApiKeyIndependently() throws Exception {
    String exhaustedKey = uniqueKey();
    for (int i = 0; i < 10; i++) {
      mockMvc.perform(get(RANDOM_QUOTE).header("X-API-Key", exhaustedKey));
    }
    mockMvc
        .perform(get(RANDOM_QUOTE).header("X-API-Key", exhaustedKey))
        .andExpect(status().isTooManyRequests());

    mockMvc
        .perform(get(RANDOM_QUOTE).header("X-API-Key", uniqueKey()))
        .andExpect(status().isOk())
        .andExpect(header().string(RateLimitFilter.REMAINING_HEADER, "9"));
  }

  @Test
  void rejectsRequestWithoutApiKeyAsUnauthorizedProblem() throws Exception {
    mockMvc
        .perform(get(RANDOM_QUOTE))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title").value("Unauthorized"))
        .andExpect(jsonPath("$.detail").value("Missing API key in header 'X-API-Key'"));
  }

  @Test
  void rejectsRequestWithBlankApiKeyAsUnauthorized() throws Exception {
    mockMvc
        .perform(get(RANDOM_QUOTE).header("X-API-Key", "   "))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void rejectsOverlongApiKeyAsUnauthorizedWithoutTrackingIt() throws Exception {
    String overlongKey = "k".repeat(RateLimitFilter.MAX_API_KEY_LENGTH + 1);

    mockMvc
        .perform(get(RANDOM_QUOTE).header("X-API-Key", overlongKey))
        .andExpect(status().isUnauthorized())
        .andExpect(header().doesNotExist(RateLimitFilter.LIMIT_HEADER))
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.detail").value("Invalid API key"));
  }

  @Test
  void acceptsApiKeyAtMaximumLength() throws Exception {
    String longestKey = uniqueKey() + "k".repeat(RateLimitFilter.MAX_API_KEY_LENGTH - 40);

    mockMvc.perform(get(RANDOM_QUOTE).header("X-API-Key", longestKey)).andExpect(status().isOk());
  }

  @Test
  void doesNotRequireApiKeyOutsideProtectedPaths() throws Exception {
    mockMvc.perform(get("/api/other")).andExpect(status().isNotFound());
  }

  private static String uniqueKey() {
    return "key-" + UUID.randomUUID();
  }
}
