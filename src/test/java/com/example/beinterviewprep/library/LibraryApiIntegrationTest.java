package com.example.beinterviewprep.library;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.beinterviewprep.common.error.ConflictException;
import com.example.beinterviewprep.library.domain.Book;
import com.example.beinterviewprep.library.domain.Loan;
import com.example.beinterviewprep.library.persistence.BookRepository;
import com.example.beinterviewprep.library.persistence.LoanRepository;
import com.example.beinterviewprep.library.service.LoanService;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
class LibraryApiIntegrationTest {

  @Autowired MockMvc mockMvc;
  @Autowired LoanService loanService;
  @Autowired BookRepository bookRepository;
  @Autowired LoanRepository loanRepository;

  @AfterEach
  void cleanUp() {
    loanRepository.deleteAll();
    bookRepository.deleteAll();
  }

  @Test
  void borrowReturnAndDeleteLifecycle() throws Exception {
    long id = createBook("Dune", "Frank Herbert", "9780441013593");

    mockMvc
        .perform(borrow(id, 42))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.bookId").value(id))
        .andExpect(jsonPath("$.memberId").value(42))
        .andExpect(jsonPath("$.returnedAt").doesNotExist());
    mockMvc.perform(get("/api/books/" + id)).andExpect(jsonPath("$.available").value(false));

    mockMvc
        .perform(borrow(id, 7))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.title").value("Conflict"))
        .andExpect(jsonPath("$.detail").value(containsString("already borrowed")));
    mockMvc.perform(delete("/api/books/" + id)).andExpect(status().isConflict());

    mockMvc
        .perform(post("/api/books/" + id + "/return"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.returnedAt").exists());
    mockMvc.perform(post("/api/books/" + id + "/return")).andExpect(status().isConflict());

    mockMvc.perform(delete("/api/books/" + id)).andExpect(status().isNoContent());
    mockMvc.perform(get("/api/books/" + id)).andExpect(status().isNotFound());
  }

  @Test
  void rejectsDuplicateIsbnOnCreateAndUpdate() throws Exception {
    createBook("Dune", "Frank Herbert", "9780441013593");
    long other = createBook("Emma", "Jane Austen", "9780141439587");

    mockMvc
        .perform(bookRequest(post("/api/books"), "Copy", "Someone", "9780441013593"))
        .andExpect(status().isConflict());
    mockMvc
        .perform(bookRequest(put("/api/books/" + other), "Emma", "Jane Austen", "9780441013593"))
        .andExpect(status().isConflict());
  }

  @Test
  void searchesByTitleOrAuthorIgnoringCase() throws Exception {
    createBook("Dune", "Frank Herbert", "9780441013593");
    createBook("Emma", "Jane Austen", "9780141439587");
    createBook("Persuasion", "Jane Austen", "9780141439686");

    mockMvc
        .perform(get("/api/books").param("q", "AUSTEN"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(2));
    mockMvc
        .perform(get("/api/books").param("q", "dun"))
        .andExpect(jsonPath("$.content[0].title").value("Dune"));
    mockMvc
        .perform(get("/api/books").param("q", "%"))
        .andExpect(jsonPath("$.totalElements").value(0));
  }

  @Test
  void rejectsSortingByUnknownPropertyWithBadRequest() throws Exception {
    mockMvc
        .perform(get("/api/books").param("q", "x").param("sort", "nope"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value(containsString("nope")));
    mockMvc.perform(get("/api/books").param("sort", "nope")).andExpect(status().isBadRequest());
    mockMvc
        .perform(get("/api/books").param("q", "x").param("sort", "title,desc"))
        .andExpect(status().isOk());
  }

  @Test
  void databaseAllowsOnlyOneOpenLoanPerBook() throws Exception {
    long id = createBook("Dune", "Frank Herbert", "9780441013593");
    Book book = bookRepository.findById(id).orElseThrow();
    loanRepository.saveAndFlush(new Loan(book, 1L, Instant.now()));

    assertThatThrownBy(() -> loanRepository.saveAndFlush(new Loan(book, 2L, Instant.now())))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("ux_loan_open_per_book");
  }

  @Test
  void onlyOneOfManyConcurrentBorrowsSucceeds() throws Exception {
    long id = createBook("Dune", "Frank Herbert", "9780441013593");
    int threads = 8;
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<Boolean>> results = new ArrayList<>();
    for (int i = 0; i < threads; i++) {
      long memberId = i + 1;
      Callable<Boolean> attempt =
          () -> {
            start.await();
            try {
              loanService.borrow(id, memberId);
              return true;
            } catch (ConflictException ex) {
              return false;
            }
          };
      results.add(pool.submit(attempt));
    }
    start.countDown();

    int successes = 0;
    for (Future<Boolean> result : results) {
      if (result.get()) {
        successes++;
      }
    }
    pool.shutdown();

    assertThat(successes).isEqualTo(1);
    assertThat(loanRepository.count()).isEqualTo(1);
  }

  private long createBook(String title, String author, String isbn) throws Exception {
    String body =
        mockMvc
            .perform(bookRequest(post("/api/books"), title, author, isbn))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return ((Number) JsonPath.read(body, "$.id")).longValue();
  }

  private static MockHttpServletRequestBuilder bookRequest(
      MockHttpServletRequestBuilder builder, String title, String author, String isbn) {
    return builder
        .contentType(MediaType.APPLICATION_JSON)
        .content(
            """
            {"title":"%s","author":"%s","isbn":"%s","publishedYear":1990}
            """
                .formatted(title, author, isbn));
  }

  private static MockHttpServletRequestBuilder borrow(long bookId, long memberId) {
    return post("/api/books/" + bookId + "/borrow")
        .contentType(MediaType.APPLICATION_JSON)
        .content(
            """
            {"memberId":%d}
            """
                .formatted(memberId));
  }
}
