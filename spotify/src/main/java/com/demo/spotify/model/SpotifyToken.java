package com.demo.spotify.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

public class SpotifyToken {

    @Getter
    @Setter
    @JsonProperty("access_token")
    private String accessToken;

    @Getter
    @Setter
    @JsonProperty("expires_in")
    private long expiresIn; // in seconds

    @JsonIgnore
    private long expiryTime;

    /**
     * Calculates the absolute time in milliseconds when this token will expire.
     * This should be called immediately after the token is received.
     */
    public void calculateExpiryTime() {
        this.expiryTime = System.currentTimeMillis() + (this.expiresIn * 1000) - 60000;
    }

    /**
     * Checks if the token has expired.
     */
    public boolean isExpired() {
        return System.currentTimeMillis() >= this.expiryTime;
    }
}