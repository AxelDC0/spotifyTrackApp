package com.demo.spotify.repository;

import com.demo.spotify.entity.Track;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TrackRepository extends JpaRepository<Track, String> {
    Optional<Track> findByIsrc(String isrc);
}