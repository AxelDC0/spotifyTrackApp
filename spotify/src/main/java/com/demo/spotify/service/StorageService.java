package com.demo.spotify.service;

import org.springframework.core.io.Resource;

public interface StorageService {

    /** A record to hold a loaded resource and its content type. */
    record StoredFile(Resource resource, String contentType) {}

    /**
     * Stores the given bytes under a specific file name.
     * @param fileBytes The raw bytes of the file.
     * @param fileName The name of the file to store.
     * @return The full path to the newly stored file.
     */
    String storeFile(byte[] fileBytes, String fileName);

    /**
     * Loads a file from storage as a resource.
     * @param filePathString The full path to the file.
     * @return A StoredFile record containing the resource and its content type.
     */
    StoredFile loadFileAsResource(String filePathString);

}