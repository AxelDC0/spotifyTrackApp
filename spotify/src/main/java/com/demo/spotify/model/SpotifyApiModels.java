package com.demo.spotify.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A container class for all DTOs/records mapping to the Spotify API responses.
 * Using nested static records keeps related models grouped and avoids polluting the package namespace.
 */
public final class SpotifyApiModels {

    // Private constructor to prevent instantiation of the container class.
    private SpotifyApiModels() {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SpotifyTrackResponse(Tracks tracks) {
        // Compact constructor for validation.
        public SpotifyTrackResponse {
            Objects.requireNonNull(tracks, "The 'tracks' field cannot be null.");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Tracks(List<TrackItem> items) {
        public Tracks {
            // Ensure the list is never null, making it safer for consumers.
            if (items == null) {
                items = Collections.emptyList();
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TrackItem(
            String name,
            boolean explicit,
            @JsonProperty("duration_ms") long durationMs,
            Album album,
            List<Artist> artists
    ) {
        public TrackItem {
            // A track must have a name and an album.
            Objects.requireNonNull(name, "Track 'name' cannot be null.");
            Objects.requireNonNull(album, "Track 'album' cannot be null.");
        }

        /** Utility method to provide the duration in whole seconds. */
        public long durationInSeconds() {
            return durationMs / 1000;
        }

        /** Safely gets the name of the first artist. */
        public Optional<String> primaryArtistName() {
            if (artists == null || artists.isEmpty()) {
                return Optional.empty();
            }
            return Optional.ofNullable(artists.get(0).name());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Album(
            String id,
            String name,
            List<Image> images
    ) {
        public Album {
            Objects.requireNonNull(id, "Album 'id' cannot be null.");
            Objects.requireNonNull(name, "Album 'name' cannot be null.");
        }

        /** Safely gets the URL of the first (and typically largest) image. */
        public Optional<String> primaryImageUrl() {
            if (images == null || images.isEmpty()) {
                return Optional.empty();
            }
            return Optional.ofNullable(images.get(0).url());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Artist(String name) {
        public Artist {
            Objects.requireNonNull(name, "Artist 'name' cannot be null.");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Image(String url) {
        public Image {
            Objects.requireNonNull(url, "Image 'url' cannot be null.");
        }
    }
}