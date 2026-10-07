package com.example.beinterviewprep.file.persistence;

import com.example.beinterviewprep.file.domain.StoredFile;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoredFileRepository extends JpaRepository<StoredFile, UUID> {}
