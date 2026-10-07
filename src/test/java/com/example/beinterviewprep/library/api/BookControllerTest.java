package com.example.beinterviewprep.library.api;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.beinterviewprep.common.error.ConflictException;
import com.example.beinterviewprep.library.domain.Book;
import com.example.beinterviewprep.library.service.BookService;
import java.time.Year;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BookController.class)
class BookControllerTest {

  private static final String VALID_BOOK =
      """
      {"title":"Dune","author":"Frank Herbert","isbn":"9780441013593","publishedYear":1965}
      """;

  @Autowired MockMvc mockMvc;

  @MockitoBean BookService bookService;

  @Test
  void createsBookAndReturnsLocation() throws Exception {
    Book book = new Book("Dune", "Frank Herbert", "9780441013593", 1965);
    ReflectionTestUtils.setField(book, "id", 7L);
    when(bookService.create(any())).thenReturn(book);

    mockMvc
        .perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content(VALID_BOOK))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/books/7"))
        .andExpect(jsonPath("$.isbn").value("9780441013593"))
        .andExpect(jsonPath("$.available").value(true));
  }

  @Test
  void rejectsInvalidBookWithFieldErrors() throws Exception {
    int nextYear = Year.now().getValue() + 1;
    String body =
        """
        {"title":" ","author":"","isbn":"12-34","publishedYear":%d}
        """
            .formatted(nextYear);

    mockMvc
        .perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title").value("Validation failed"))
        .andExpect(jsonPath("$.errors.title").exists())
        .andExpect(jsonPath("$.errors.author").exists())
        .andExpect(jsonPath("$.errors.isbn").exists())
        .andExpect(jsonPath("$.errors.publishedYear").value("must not be in the future"));
    verifyNoInteractions(bookService);
  }

  @Test
  void returnsConflictForDuplicateIsbn() throws Exception {
    when(bookService.create(any()))
        .thenThrow(new ConflictException("A book with ISBN 9780441013593 already exists"));

    mockMvc
        .perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content(VALID_BOOK))
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.detail").value(containsString("already exists")));
  }

  @Test
  void returnsConflictWhenDeletingBorrowedBook() throws Exception {
    doThrow(new ConflictException("Book 3 is currently borrowed and cannot be deleted"))
        .when(bookService)
        .delete(3L);

    mockMvc
        .perform(delete("/api/books/3"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.detail").value(containsString("currently borrowed")));
  }
}
