package com.demo.spotify.controller;

import com.demo.spotify.dto.TrackResponseDTO;
import com.demo.spotify.entity.Track;
import com.demo.spotify.mapper.TrackMapper;
import com.demo.spotify.service.StorageService;
import com.demo.spotify.service.TrackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tracks")
@Tag(name = "Tracks", description = "Endpoints for managing music track data.")
@Validated
public class TrackController {

    private static final Logger log = LoggerFactory.getLogger(TrackController.class);

    private final TrackService trackService;
    private final StorageService storageService;
    private final TrackMapper trackMapper;

    public TrackController(TrackService trackService, StorageService storageService, TrackMapper trackMapper) {
        this.trackService = trackService;
        this.storageService = storageService;
        this.trackMapper = trackMapper;
    }

    @PostMapping
    @Operation(summary = "Creates a new track by ISRC")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Track created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid ISRC format provided"),
            @ApiResponse(responseCode = "404", description = "Track not found on external service")
    })
    public ResponseEntity<TrackResponseDTO> createTrack(
            @RequestParam @NotBlank @Pattern(regexp = "^[A-Z]{2}[A-Z0-9]{3}\\d{7}$", message = "Invalid ISRC format") String isrc) {
        log.info("Request received to create track with ISRC: {}", isrc);
        Track createdTrack = trackService.createTrack(isrc);
        log.info("Successfully created track with ISRC: {}", isrc);
        return ResponseEntity.status(HttpStatus.CREATED).body(trackMapper.toDto(createdTrack));
    }

    @GetMapping("/{isrc}")
    @Operation(summary = "Retrieves track metadata by ISRC")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved track metadata"),
            @ApiResponse(responseCode = "404", description = "Track not found in the database")
    })
    public ResponseEntity<TrackResponseDTO> getTrackMetadata(@PathVariable String isrc) {
        log.info("Request received for metadata of track with ISRC: {}", isrc);
        Track track = trackService.getTrackByIsrc(isrc);
        return ResponseEntity.ok(trackMapper.toDto(track));
    }


    @GetMapping("/{isrc}/cover")
    @Operation(summary = "Downloads the track's cover image by ISRC")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved cover image"),
            @ApiResponse(responseCode = "404", description = "Cover image or track not found")
    })
    public ResponseEntity<Resource> getCover(@PathVariable String isrc) {
        log.info("Request received for cover image of track with ISRC: {}", isrc);
        Track track = trackService.getTrackByIsrc(isrc);

        StorageService.StoredFile storedFile = storageService.loadFileAsResource(track.getCoverImagePath());
        Resource resource = storedFile.resource();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, storedFile.contentType())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}