package com.example.beinterviewprep.file.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.beinterviewprep.file.persistence.StoredFileRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class FileApiTest {

  private static final byte[] PNG_SIGNATURE = {
    (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
  };
  private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
  private static final byte[] PDF_SIGNATURE = "%PDF-1.7\n".getBytes();
  private static final byte[] EXE_SIGNATURE = {0x4D, 0x5A, (byte) 0x90, 0x00, 0x03, 0x00};

  @TempDir static Path tempDir;

  @DynamicPropertySource
  static void storageProperties(DynamicPropertyRegistry registry) {
    registry.add("app.file-storage.base-dir", () -> storageDir().toString());
  }

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;
  @Autowired StoredFileRepository repository;

  @AfterEach
  void cleanUp() throws Exception {
    repository.deleteAll();
    for (Path stored : storedFiles()) {
      Files.delete(stored);
    }
  }

  @Test
  void uploadsPngJpegAndPdfWithDetectedContentType() throws Exception {
    upload(file("logo.png", "application/octet-stream", bytesStartingWith(PNG_SIGNATURE, 100)))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", containsString("/api/files/")))
        .andExpect(jsonPath("$.originalName").value("logo.png"))
        .andExpect(jsonPath("$.contentType").value("image/png"))
        .andExpect(jsonPath("$.size").value(PNG_SIGNATURE.length + 100))
        .andExpect(jsonPath("$.uploadedAt").exists());
    upload(file("photo.jpeg", "image/jpeg", bytesStartingWith(JPEG_SIGNATURE, 10)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.contentType").value("image/jpeg"));
    upload(file("report.pdf", "application/pdf", bytesStartingWith(PDF_SIGNATURE, 10)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.contentType").value("application/pdf"));

    assertThat(repository.count()).isEqualTo(3);
    assertThat(storedFiles()).hasSize(3);
  }

  @Test
  void rejectsExecutableRenamedToPng() throws Exception {
    upload(file("photo.png", "image/png", bytesStartingWith(EXE_SIGNATURE, 50)))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title").value("Unsupported file type"))
        .andExpect(jsonPath("$.detail", containsString("JPEG, PNG and PDF")));

    assertThat(repository.count()).isZero();
    assertThat(storedFiles()).isEmpty();
  }

  @Test
  void rejectsDisallowedTypeEvenWithAllowedClientContentType() throws Exception {
    upload(file("notes.txt", "application/pdf", "just some text".getBytes()))
        .andExpect(status().isUnsupportedMediaType());
  }

  @Test
  void rejectsExtensionThatDisagreesWithContent() throws Exception {
    upload(file("invoice.pdf", "application/pdf", bytesStartingWith(PNG_SIGNATURE, 10)))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.detail", containsString("does not match")));
  }

  @Test
  void rejectsFileLargerThanFiveMegabytesWithPayloadTooLarge() throws Exception {
    byte[] oversized = bytesStartingWith(PNG_SIGNATURE, 5 * 1024 * 1024 + 1 - PNG_SIGNATURE.length);

    upload(file("big.png", "image/png", oversized))
        .andExpect(status().isPayloadTooLarge())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.detail", containsString("5 MB")));

    assertThat(storedFiles()).isEmpty();
  }

  @Test
  void acceptsFileOfExactlyFiveMegabytes() throws Exception {
    byte[] exact = bytesStartingWith(PNG_SIGNATURE, 5 * 1024 * 1024 - PNG_SIGNATURE.length);

    upload(file("exact.png", "image/png", exact)).andExpect(status().isCreated());
  }

  @Test
  void rejectsEmptyFile() throws Exception {
    upload(file("empty.png", "image/png", new byte[0])).andExpect(status().isBadRequest());
  }

  @Test
  void storesTraversalFileNameSafelyInsideStorageDirectory() throws Exception {
    JsonNode body =
        json(
            upload(file("../../../escape.png", "image/png", bytesStartingWith(PNG_SIGNATURE, 10)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalName").value("escape.png")));

    assertThat(storedFiles())
        .singleElement()
        .satisfies(
            stored -> {
              assertThat(stored.getParent()).isEqualTo(storageDir());
              assertThat(stored.getFileName().toString()).isEqualTo(body.get("id").asText());
            });
    try (Stream<Path> all = Files.walk(tempDir)) {
      assertThat(all.filter(p -> p.getFileName().toString().contains("escape"))).isEmpty();
    }
  }

  @Test
  void downloadsFileWithOriginalNameAndContent() throws Exception {
    byte[] pdf = bytesStartingWith(PDF_SIGNATURE, 32);
    String id = json(upload(file("Q3 résumé.pdf", "application/pdf", pdf))).get("id").asText();

    mockMvc
        .perform(get("/api/files/{id}/download", id))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_PDF))
        .andExpect(header().string("Content-Disposition", containsString("attachment")))
        .andExpect(
            header()
                .string("Content-Disposition", containsString("filename*=UTF-8''Q3%20r%C3%A9sum")))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andExpect(content().bytes(pdf));
  }

  @Test
  void listsUploadedFilesWithMetadata() throws Exception {
    upload(file("a.png", "image/png", bytesStartingWith(PNG_SIGNATURE, 1)));
    upload(file("b.pdf", "application/pdf", bytesStartingWith(PDF_SIGNATURE, 1)));

    mockMvc
        .perform(get("/api/files"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.content[0].originalName").value("b.pdf"))
        .andExpect(jsonPath("$.content[1].originalName").value("a.png"))
        .andExpect(jsonPath("$.content[1].contentType").value("image/png"))
        .andExpect(jsonPath("$.content[1].size").value(PNG_SIGNATURE.length + 1));
  }

  @Test
  void deleteRemovesStoredFileAndRecord() throws Exception {
    String id =
        json(upload(file("gone.png", "image/png", bytesStartingWith(PNG_SIGNATURE, 10))))
            .get("id")
            .asText();

    mockMvc.perform(delete("/api/files/{id}", id)).andExpect(status().isNoContent());

    assertThat(repository.existsById(UUID.fromString(id))).isFalse();
    assertThat(storageDir().resolve(id)).doesNotExist();
    mockMvc.perform(get("/api/files/{id}", id)).andExpect(status().isNotFound());
    mockMvc.perform(get("/api/files/{id}/download", id)).andExpect(status().isNotFound());
  }

  @Test
  void returnsNotFoundForUnknownFileAndBadRequestForMalformedId() throws Exception {
    mockMvc.perform(delete("/api/files/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
    mockMvc.perform(get("/api/files/not-a-uuid")).andExpect(status().isBadRequest());
  }

  private ResultActions upload(MockMultipartFile file) throws Exception {
    return mockMvc.perform(multipart("/api/files").file(file));
  }

  private JsonNode json(ResultActions result) throws Exception {
    return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
  }

  private static MockMultipartFile file(String name, String contentType, byte[] bytes) {
    return new MockMultipartFile("file", name, contentType, bytes);
  }

  private static byte[] bytesStartingWith(byte[] signature, int extraBytes) {
    byte[] bytes = Arrays.copyOf(signature, signature.length + extraBytes);
    Arrays.fill(bytes, signature.length, bytes.length, (byte) 7);
    return bytes;
  }

  private static Path storageDir() {
    return tempDir.resolve("files").toAbsolutePath().normalize();
  }

  private static List<Path> storedFiles() throws Exception {
    if (!Files.isDirectory(storageDir())) {
      return List.of();
    }
    try (Stream<Path> files = Files.list(storageDir())) {
      return files.toList();
    }
  }
}
