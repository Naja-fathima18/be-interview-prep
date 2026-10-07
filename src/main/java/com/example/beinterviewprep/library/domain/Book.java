package com.example.beinterviewprep.library.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "book")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Book {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false)
  private String author;

  @Column(nullable = false, unique = true)
  private String isbn;

  @Column(name = "published_year", nullable = false)
  private int publishedYear;

  @Column(nullable = false)
  private boolean borrowed;

  public Book(String title, String author, String isbn, int publishedYear) {
    updateDetails(title, author, isbn, publishedYear);
  }

  public void updateDetails(String title, String author, String isbn, int publishedYear) {
    this.title = title;
    this.author = author;
    this.isbn = isbn;
    this.publishedYear = publishedYear;
  }

  public void markBorrowed() {
    this.borrowed = true;
  }

  public void markReturned() {
    this.borrowed = false;
  }
}
