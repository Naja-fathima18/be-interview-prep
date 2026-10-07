package com.example.beinterviewprep.library.persistence;

import com.example.beinterviewprep.library.domain.Loan;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanRepository extends JpaRepository<Loan, Long> {

  Optional<Loan> findByBookIdAndReturnedAtIsNull(Long bookId);
}
