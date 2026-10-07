package com.example.beinterviewprep.ratelimit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.beinterviewprep.quote.api.QuoteController;
import com.example.beinterviewprep.quote.service.QuoteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = QuoteController.class,
    properties = {"rate-limit.limit=3", "rate-limit.window=30s", "rate-limit.header=X-Client-Key"})
@Import({RateLimitConfig.class, QuoteService.class})
@EnableConfigurationProperties(RateLimitProperties.class)
class RateLimitPropertiesOverrideTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void appliesLimitWindowAndHeaderFromConfiguration() throws Exception {
    for (int i = 0; i < 3; i++) {
      mockMvc
          .perform(get("/api/quotes/random").header("X-Client-Key", "override-key"))
          .andExpect(status().isOk())
          .andExpect(header().string(RateLimitFilter.LIMIT_HEADER, "3"));
    }

    mockMvc
        .perform(get("/api/quotes/random").header("X-Client-Key", "override-key"))
        .andExpect(status().isTooManyRequests())
        .andExpect(RetryAfterMatchers.retryAfterCloseTo(30));
  }

  @Test
  void ignoresDefaultHeaderWhenAnotherIsConfigured() throws Exception {
    mockMvc
        .perform(get("/api/quotes/random").header("X-API-Key", "some-key"))
        .andExpect(status().isUnauthorized());
  }
}
