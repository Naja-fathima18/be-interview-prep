package com.example.beinterviewprep.library.api;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.beinterviewprep.common.error.ConflictException;
import com.example.beinterviewprep.library.service.LoanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LoanController.class)
class LoanControllerTest {

  @Autowired MockMvc mockMvc;

  @MockitoBean LoanService loanService;

  @Test
  void returnsConflictProblemWhenBorrowingUnavailableBook() throws Exception {
    when(loanService.borrow(1L, 42L))
        .thenThrow(
            new ConflictException(
                "Book 1 is already borrowed and is unavailable until it is returned"));

    mockMvc
        .perform(
            post("/api/books/1/borrow")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"memberId":42}
                    """))
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title").value("Conflict"))
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(
            jsonPath("$.detail")
                .value("Book 1 is already borrowed and is unavailable until it is returned"));
  }

  @Test
  void rejectsBorrowWithoutMemberId() throws Exception {
    mockMvc
        .perform(post("/api/books/1/borrow").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.memberId").exists());
    verifyNoInteractions(loanService);
  }

  @Test
  void returnsConflictWhenReturningBookThatIsNotBorrowed() throws Exception {
    when(loanService.giveBack(5L))
        .thenThrow(new ConflictException("Book 5 is not currently borrowed"));

    mockMvc
        .perform(post("/api/books/5/return"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.detail").value("Book 5 is not currently borrowed"));
  }
}
