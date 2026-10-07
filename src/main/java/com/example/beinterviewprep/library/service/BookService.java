package com.example.beinterviewprep.library.service;

import com.example.beinterviewprep.common.error.BadRequestException;
import com.example.beinterviewprep.common.error.ConflictException;
import com.example.beinterviewprep.common.error.NotFoundException;
import com.example.beinterviewprep.library.api.BookRequest;
import com.example.beinterviewprep.library.domain.Book;
import com.example.beinterviewprep.library.persistence.BookRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookService {

  private static final Set<String> SORTABLE_PROPERTIES =
      new LinkedHashSet<>(List.of("id", "title", "author", "isbn", "publishedYear"));

  private static final String ISBN_CONSTRAINT = "uk_book_isbn";

  private final BookRepository bookRepository;

  @Transactional
  public Book create(BookRequest request) {
    if (bookRepository.existsByIsbn(request.isbn())) {
      throw duplicateIsbn(request.isbn());
    }
    Book book =
        new Book(request.title(), request.author(), request.isbn(), request.publishedYear());
    return saveEnforcingUniqueIsbn(book);
  }

  @Transactional(readOnly = true)
  public Page<Book> search(String query, Pageable pageable) {
    rejectUnsortableProperties(pageable.getSort());
    if (query == null || query.isBlank()) {
      return bookRepository.findAll(pageable);
    }
    return bookRepository.searchByTitleOrAuthor(containsPattern(query), pageable);
  }

  @Transactional(readOnly = true)
  public Book get(Long id) {
    return bookRepository.findById(id).orElseThrow(() -> bookNotFound(id));
  }

  @Transactional
  public Book update(Long id, BookRequest request) {
    Book book = lockBook(id);
    if (bookRepository.existsByIsbnAndIdNot(request.isbn(), id)) {
      throw duplicateIsbn(request.isbn());
    }
    book.updateDetails(request.title(), request.author(), request.isbn(), request.publishedYear());
    return saveEnforcingUniqueIsbn(book);
  }

  @Transactional
  public void delete(Long id) {
    Book book = lockBook(id);
    if (book.isBorrowed()) {
      throw new ConflictException("Book " + id + " is currently borrowed and cannot be deleted");
    }
    bookRepository.delete(book);
  }

  private Book lockBook(Long id) {
    return bookRepository.findByIdForUpdate(id).orElseThrow(() -> bookNotFound(id));
  }

  private Book saveEnforcingUniqueIsbn(Book book) {
    try {
      return bookRepository.saveAndFlush(book);
    } catch (DataIntegrityViolationException ex) {
      if (ConstraintViolations.violates(ex, ISBN_CONSTRAINT)) {
        throw duplicateIsbn(book.getIsbn());
      }
      throw ex;
    }
  }

  private static void rejectUnsortableProperties(Sort sort) {
    sort.stream()
        .map(Sort.Order::getProperty)
        .filter(property -> !SORTABLE_PROPERTIES.contains(property))
        .findFirst()
        .ifPresent(
            property -> {
              throw new BadRequestException(
                  "Cannot sort by '" + property + "'; allowed: " + SORTABLE_PROPERTIES);
            });
  }

  private static String containsPattern(String query) {
    String escaped =
        query
            .strip()
            .toLowerCase(Locale.ROOT)
            .replace("!", "!!")
            .replace("%", "!%")
            .replace("_", "!_");
    return "%" + escaped + "%";
  }

  private static ConflictException duplicateIsbn(String isbn) {
    return new ConflictException("A book with ISBN " + isbn + " already exists");
  }

  private static NotFoundException bookNotFound(Long id) {
    return new NotFoundException("Book " + id + " not found");
  }
}
