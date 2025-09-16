package com.demo.spotify.mapper;

import com.demo.spotify.dto.TrackResponseDTO;
import com.demo.spotify.entity.Track;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Component
public class TrackMapper {

    public TrackResponseDTO toDto(Track track) {
        if (track == null) {
            return null;
        }

        String coverUrl = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/v1/tracks/{isrc}/cover")
                .buildAndExpand(track.getIsrc())
                .toUriString();

        return new TrackResponseDTO(
                track.getIsrc(),
                track.getName(),
                track.getArtistName(),
                track.getAlbumName(),
                track.isExplicit(),
                track.getPlaybackSeconds(),
                coverUrl
        );
    }
}