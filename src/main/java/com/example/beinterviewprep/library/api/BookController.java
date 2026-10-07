package com.example.beinterviewprep.library.api;

import com.example.beinterviewprep.library.service.BookService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

  private final BookService bookService;

  @PostMapping
  public ResponseEntity<BookResponse> create(@Valid @RequestBody BookRequest request) {
    BookResponse created = BookResponse.from(bookService.create(request.toCommand()));
    return ResponseEntity.created(URI.create("/api/books/" + created.id())).body(created);
  }

  @GetMapping
  public PageResponse<BookResponse> list(
      @RequestParam(required = false) @Size(max = 255) String q,
      @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
    return PageResponse.from(bookService.search(q, pageable), BookResponse::from);
  }

  @GetMapping("/{id}")
  public BookResponse get(@PathVariable Long id) {
    return BookResponse.from(bookService.get(id));
  }

  @PutMapping("/{id}")
  public BookResponse update(@PathVariable Long id, @Valid @RequestBody BookRequest request) {
    return BookResponse.from(bookService.update(id, request.toCommand()));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    bookService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
