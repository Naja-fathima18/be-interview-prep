package com.example.beinterviewprep.quote.service;

import com.example.beinterviewprep.quote.domain.Quote;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;

@Service
public class QuoteService {

  private static final List<Quote> QUOTES =
      List.of(
          new Quote("Simplicity is prerequisite for reliability.", "Edsger W. Dijkstra"),
          new Quote("Premature optimization is the root of all evil.", "Donald Knuth"),
          new Quote("Talk is cheap. Show me the code.", "Linus Torvalds"),
          new Quote("Make it work, make it right, make it fast.", "Kent Beck"),
          new Quote("Any fool can write code that a computer can understand.", "Martin Fowler"));

  public Quote randomQuote() {
    return QUOTES.get(ThreadLocalRandom.current().nextInt(QUOTES.size()));
  }

  public List<Quote> allQuotes() {
    return QUOTES;
  }
}
