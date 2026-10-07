package com.example.beinterviewprep.file.api;

import com.example.beinterviewprep.file.service.StoredFileView;
import java.util.List;
import org.springframework.data.domain.Page;

public record FilePageResponse(
    List<FileResponse> content, int page, int size, long totalElements, int totalPages) {

  static FilePageResponse from(Page<StoredFileView> page) {
    return new FilePageResponse(
        page.map(FileResponse::from).getContent(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages());
  }
}
