package com.example.beinterviewprep.library.persistence;

import com.example.beinterviewprep.library.domain.Book;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {

  boolean existsByIsbn(String isbn);

  boolean existsByIsbnAndIdNot(String isbn, Long id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select b from Book b where b.id = :id")
  Optional<Book> findByIdForUpdate(@Param("id") Long id);

  @Query(
      """
      select b from Book b
      where lower(b.title) like :pattern escape '!'
         or lower(b.author) like :pattern escape '!'
      """)
  Page<Book> searchByTitleOrAuthor(@Param("pattern") String pattern, Pageable pageable);
}
