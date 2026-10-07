package com.example.beinterviewprep.library.api;

import com.example.beinterviewprep.library.domain.Book;

public record BookResponse(
    Long id, String title, String author, String isbn, int publishedYear, boolean available) {

  public static BookResponse from(Book book) {
    return new BookResponse(
        book.getId(),
        book.getTitle(),
        book.getAuthor(),
        book.getIsbn(),
        book.getPublishedYear(),
        !book.isBorrowed());
  }
}
