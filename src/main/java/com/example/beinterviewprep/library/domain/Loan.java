package com.example.beinterviewprep.library.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "loan")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Loan {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "book_id", nullable = false)
  private Book book;

  @Column(name = "member_id", nullable = false)
  private Long memberId;

  @Column(name = "borrowed_at", nullable = false)
  private Instant borrowedAt;

  @Column(name = "returned_at")
  private Instant returnedAt;

  public Loan(Book book, Long memberId, Instant borrowedAt) {
    this.book = book;
    this.memberId = memberId;
    this.borrowedAt = borrowedAt;
  }

  public void close(Instant returnedAt) {
    this.returnedAt = returnedAt;
  }
}
