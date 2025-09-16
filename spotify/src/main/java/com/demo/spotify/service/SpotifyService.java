package com.demo.spotify.service;

import com.demo.spotify.entity.Track;
import com.demo.spotify.exception.SpotifyApiException;
import com.demo.spotify.external.SpotifyApiClient;
import com.demo.spotify.model.SpotifyApiModels.Album;
import com.demo.spotify.model.SpotifyApiModels.TrackItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class SpotifyService {

    private static final Logger log = LoggerFactory.getLogger(SpotifyService.class);

    private final SpotifyApiClient spotifyApiClient;
    private final RestTemplate restTemplate;
    private final StorageService storageService; // Depends on the interface

    // Constructor injection remains the same, but the coverImageBasePath is removed.
    public SpotifyService(SpotifyApiClient spotifyApiClient, RestTemplate restTemplate, StorageService storageService) {
        this.spotifyApiClient = spotifyApiClient;
        this.restTemplate = restTemplate;
        this.storageService = storageService;
    }

    // This method now benefits from caching at the ApiClient level.
    public Track fetchTrackMetadata(String isrc) {
        log.debug("Fetching track metadata from Spotify for ISRC: {}", isrc);
        // The ApiClient is now responsible for returning a POJO.
        TrackItem trackItem = spotifyApiClient.getTrackByIsrc(isrc);
        return mapPojosToTrack(trackItem, isrc);
    }

    // This method also benefits from caching at the ApiClient level.
    public String fetchAndStoreCoverImage(String albumId, String isrc) {
        log.debug("Fetching album details from Spotify for album ID: {}", albumId);
        Album album = spotifyApiClient.getAlbumById(albumId);

        if (album.images() == null || album.images().isEmpty() || album.images().get(0).url().isBlank()) {
            log.error("Spotify API returned no valid images for album ID: {}", albumId);
            throw new SpotifyApiException("Could not find cover image URL for album: " + albumId);
        }
        String imageUrl = album.images().get(0).url();

        byte[] imageBytes = downloadImageWithRetry(imageUrl);

        String fileName = isrc + ".jpg";
        // The service no longer needs to know about the base path.
        return storageService.storeFile(imageBytes, fileName);
    }

    /**
     * Downloads an image, with retries on failure.
     */
    @Retryable(
            value = {RestClientException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000) // Wait 1 second between retries
    )
    private byte[] downloadImageWithRetry(String imageUrl) {
        log.info("Downloading cover image from URL: {}", imageUrl);
        byte[] imageBytes = restTemplate.getForObject(imageUrl, byte[].class);
        if (imageBytes == null) {
            throw new SpotifyApiException("Failed to download image; response was empty for URL: " + imageUrl);
        }
        return imageBytes;
    }

    /**
     * Maps the API response POJOs to our internal Track entity.
     */
    private Track mapPojosToTrack(TrackItem trackItem, String isrc) {
        Track track = new Track();
        track.setIsrc(isrc);
        track.setName(trackItem.name());
        track.setAlbumName(trackItem.album().name());
        track.setAlbumId(trackItem.album().id());
        track.setExplicit(trackItem.explicit());
        track.setPlaybackSeconds(trackItem.durationMs() / 1000);

        if (trackItem.artists() != null && !trackItem.artists().isEmpty()) {
            track.setArtistName(trackItem.artists().get(0).name());
        } else {
            track.setArtistName("Unknown Artist");
        }
        return track;
    }
}