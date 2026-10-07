package com.example.beinterviewprep.quote.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class QuoteServiceTest {

  private final QuoteService quoteService = new QuoteService();

  @Test
  void returnsQuoteFromInMemoryList() {
    assertThat(quoteService.allQuotes()).contains(quoteService.randomQuote());
  }
}
