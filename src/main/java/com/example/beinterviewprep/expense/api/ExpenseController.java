package com.example.beinterviewprep.expense.api;

import com.example.beinterviewprep.expense.domain.Category;
import com.example.beinterviewprep.expense.service.ExpenseService;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.time.YearMonth;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

  private final ExpenseService expenseService;

  @PostMapping
  public ResponseEntity<ExpenseResponse> create(@Valid @RequestBody ExpenseRequest request) {
    ExpenseResponse created = ExpenseResponse.from(expenseService.create(request.toCommand()));
    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.id())
            .toUri();
    return ResponseEntity.created(location).body(created);
  }

  @GetMapping("/{id}")
  public ExpenseResponse get(@PathVariable Long id) {
    return ExpenseResponse.from(expenseService.get(id));
  }

  @GetMapping
  public ExpensePageResponse list(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(required = false) Category category,
      @PageableDefault(size = 50, sort = "date", direction = Sort.Direction.DESC)
          Pageable pageable) {
    return ExpensePageResponse.from(
        expenseService.list(from, to, category, pageable).map(ExpenseResponse::from));
  }

  @GetMapping("/summary")
  public MonthlySummaryResponse summary(@RequestParam YearMonth month) {
    return MonthlySummaryResponse.from(expenseService.summarize(month));
  }

  @PutMapping("/{id}")
  public ExpenseResponse update(@PathVariable Long id, @Valid @RequestBody ExpenseRequest request) {
    return ExpenseResponse.from(expenseService.update(id, request.toCommand()));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    expenseService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
