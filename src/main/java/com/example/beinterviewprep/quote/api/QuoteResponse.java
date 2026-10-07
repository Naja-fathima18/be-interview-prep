package com.example.beinterviewprep.quote.api;

import com.example.beinterviewprep.quote.domain.Quote;

public record QuoteResponse(String text, String author) {

  static QuoteResponse from(Quote quote) {
    return new QuoteResponse(quote.text(), quote.author());
  }
}
