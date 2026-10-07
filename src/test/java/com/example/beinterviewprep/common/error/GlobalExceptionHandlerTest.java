package com.example.beinterviewprep.common.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.ProbeController.class)
@Import(GlobalExceptionHandlerTest.ProbeController.class)
class GlobalExceptionHandlerTest {

  @Autowired MockMvc mockMvc;

  @Test
  void reportsInvalidQueryParameterInTheSameShapeAsBodyValidation() throws Exception {
    mockMvc
        .perform(get("/probe").param("q", "too-long"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Validation failed"))
        .andExpect(jsonPath("$.errors.q").exists());
  }

  @Test
  void mapsNotFoundExceptionToProblemDetail() throws Exception {
    mockMvc
        .perform(get("/probe/missing"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Resource not found"))
        .andExpect(jsonPath("$.detail").value("Thing missing not found"));
  }

  @Test
  void rejectsMalformedPathVariableWithBadRequest() throws Exception {
    mockMvc
        .perform(get("/probe/number/abc"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @RestController
  static class ProbeController {

    @GetMapping("/probe")
    String search(@RequestParam @Size(max = 3) String q) {
      return q;
    }

    @GetMapping("/probe/{name}")
    String find(@PathVariable String name) {
      throw new NotFoundException("Thing " + name + " not found");
    }

    @GetMapping("/probe/number/{id}")
    String number(@PathVariable long id) {
      return String.valueOf(id);
    }
  }
}
