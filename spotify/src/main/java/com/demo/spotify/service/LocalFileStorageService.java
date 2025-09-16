package com.demo.spotify.service;

import com.demo.spotify.exception.FileStorageException;
import com.demo.spotify.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class LocalFileStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorageService.class);

    private final Path rootLocation;

    public LocalFileStorageService(@Value("${storage.location}") String storageLocation) {
        if (storageLocation.isBlank()) {
            throw new FileStorageException("File upload location cannot be empty.");
        }
        this.rootLocation = Paths.get(storageLocation);
        try {
            Files.createDirectories(rootLocation);
            log.info("Storage directory initialized at: {}", rootLocation.toAbsolutePath());
        } catch (IOException e) {
            throw new FileStorageException("Could not initialize storage location: " + storageLocation, e);
        }
    }

    @Override
    public StoredFile loadFileAsResource(String filePathString) {
        log.debug("Attempting to load file from path: {}", filePathString);
        if (filePathString == null || filePathString.isBlank()) {
            throw new IllegalArgumentException("File path string cannot be null or empty.");
        }
        try {
            Path filePath = Paths.get(filePathString);
            Resource resource = new FileSystemResource(filePath);

            if (!resource.exists() || !resource.isReadable()) {
                log.warn("Attempted to access non-existent or unreadable file: {}", filePathString);
                throw new ResourceNotFoundException("File not found or cannot be read: " + filePathString);
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            log.info("Successfully loaded file '{}' with content type '{}'", filePathString, contentType);
            return new StoredFile(resource, contentType);
        } catch (IOException ex) {
            throw new FileStorageException("Could not determine file type for: " + filePathString, ex);
        }
    }

    @Override
    public String storeFile(byte[] fileBytes, String fileName) {
        // Sanitize file name to prevent security vulnerabilities.
        String cleanFileName = StringUtils.cleanPath(fileName);
        log.debug("Storing file with sanitized name: {}", cleanFileName);

        if (cleanFileName.contains("..")) {
            throw new FileStorageException("Cannot store file with relative path outside current directory: " + fileName);
        }

        try {
            Path targetLocation = this.rootLocation.resolve(cleanFileName);

            // Additional security check to ensure the file is stored within the root location.
            if (!targetLocation.getParent().equals(this.rootLocation)) {
                throw new FileStorageException("Cannot store file outside the main storage directory.");
            }

            Files.write(targetLocation, fileBytes);
            log.info("Successfully stored file at: {}", targetLocation);
            return targetLocation.toAbsolutePath().toString();
        } catch (IOException ex) {
            log.error("Failed to store file {}: {}", fileName, ex.getMessage());
            throw new FileStorageException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }
}