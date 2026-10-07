package com.example.beinterviewprep.quote.api;

import com.example.beinterviewprep.quote.service.QuoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quotes")
@RequiredArgsConstructor
public class QuoteController {

  private final QuoteService quoteService;

  @GetMapping("/random")
  public QuoteResponse random() {
    return QuoteResponse.from(quoteService.randomQuote());
  }
}
