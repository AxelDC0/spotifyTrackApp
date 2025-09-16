package com.demo.spotify.dto;

public record TrackResponseDTO(
        String isrc,
        String name,
        String artistName,
        String albumName,
        boolean isExplicit,
        long playbackSeconds,
        String coverImageUrl
) {
}