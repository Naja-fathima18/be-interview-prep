package com.example.beinterviewprep.booking.persistence;

import com.example.beinterviewprep.booking.domain.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {}
