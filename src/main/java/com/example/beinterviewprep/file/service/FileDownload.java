package com.example.beinterviewprep.file.service;

import org.springframework.core.io.Resource;

public record FileDownload(StoredFileView metadata, Resource content) {}
