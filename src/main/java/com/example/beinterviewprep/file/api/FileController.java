package com.example.beinterviewprep.file.api;

import com.example.beinterviewprep.file.service.FileDownload;
import com.example.beinterviewprep.file.service.FileService;
import com.example.beinterviewprep.file.service.StoredFileView;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

  private final FileService fileService;

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<FileResponse> upload(@RequestParam("file") MultipartFile file) {
    FileResponse body = FileResponse.from(fileService.upload(file));
    return ResponseEntity.created(URI.create("/api/files/" + body.id())).body(body);
  }

  @GetMapping
  public FilePageResponse list(
      @PageableDefault(size = 20, sort = "uploadedAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    return FilePageResponse.from(fileService.list(pageable));
  }

  @GetMapping("/{id}")
  public FileResponse get(@PathVariable UUID id) {
    return FileResponse.from(fileService.get(id));
  }

  @GetMapping("/{id}/download")
  public ResponseEntity<Resource> download(@PathVariable UUID id) {
    FileDownload download = fileService.download(id);
    StoredFileView metadata = download.metadata();
    ContentDisposition disposition =
        ContentDisposition.attachment()
            .filename(metadata.originalName(), StandardCharsets.UTF_8)
            .build();
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(metadata.contentType()))
        .contentLength(metadata.size())
        .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
        .header("X-Content-Type-Options", "nosniff")
        .body(download.content());
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    fileService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
