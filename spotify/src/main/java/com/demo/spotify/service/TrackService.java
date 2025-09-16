package com.demo.spotify.service;

import com.demo.spotify.entity.Track;
import com.demo.spotify.exception.ResourceNotFoundException;
import com.demo.spotify.repository.TrackRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class TrackService {

    private static final Logger log = LoggerFactory.getLogger(TrackService.class);

    private final TrackRepository trackRepository;
    private final SpotifyService spotifyService;

    public TrackService(TrackRepository trackRepository, SpotifyService spotifyService) {
        this.trackRepository = trackRepository;
        this.spotifyService = spotifyService;
    }

    /**
     * Retrieves a track by its ISRC.
     *
     * @param isrc The ISRC of the track.
     * @return The Track entity.
     * @throws ResourceNotFoundException if no track with the given ISRC is found.
     */
    public Track getTrackByIsrc(String isrc) {
        log.debug("Attempting to find track with ISRC: {}", isrc);
        return trackRepository.findByIsrc(isrc)
                .orElseThrow(() -> {
                    log.warn("Track not found in database for ISRC: {}", isrc);
                    return new ResourceNotFoundException("Track not found with ISRC: " + isrc);
                });
    }

    /**
     * Creates a new track by fetching its data from an external service if it doesn't already exist.
     * The operation is transactional to ensure data consistency.
     *
     * @param isrc The ISRC of the track to create.
     * @return The newly created or existing Track entity.
     */
    @Transactional
    public Track createTrack(String isrc) {
        return trackRepository.findByIsrc(isrc)
                .orElseGet(() -> {
                    log.info("Track with ISRC '{}' not found in DB. Fetching from external service.", isrc);
                    return createNewTrackFromSpotify(isrc);
                });
    }

    /**
     * Private helper method to encapsulate the logic of creating a new track.
     */
    private Track createNewTrackFromSpotify(String isrc) {
        Track newTrack = spotifyService.fetchTrackMetadata(isrc);

        log.info("Successfully fetched metadata for ISRC: {}. Now fetching cover image.", isrc);

        String coverImagePath = spotifyService.fetchAndStoreCoverImage(newTrack.getAlbumId(), newTrack.getIsrc());
        newTrack.setCoverImagePath(coverImagePath);

        log.info("Saving new track with ISRC '{}' to the database.", isrc);
        return trackRepository.save(newTrack);
    }
}