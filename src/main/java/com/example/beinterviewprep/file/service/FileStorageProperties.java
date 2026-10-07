package com.example.beinterviewprep.file.service;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "app.file-storage")
public record FileStorageProperties(
    @DefaultValue("storage/files") Path baseDir, @DefaultValue("5MB") DataSize maxFileSize) {}
