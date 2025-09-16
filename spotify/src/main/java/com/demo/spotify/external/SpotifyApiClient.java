package com.demo.spotify.external;

import com.demo.spotify.exception.SpotifyApiException;
import com.demo.spotify.model.SpotifyApiModels.Album;
import com.demo.spotify.model.SpotifyApiModels.SpotifyTrackResponse;
import com.demo.spotify.model.SpotifyApiModels.TrackItem;
import com.demo.spotify.model.SpotifyToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class SpotifyApiClient {

    private static final Logger log = LoggerFactory.getLogger(SpotifyApiClient.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper; // Add ObjectMapper
    private final String clientId;
    private final String clientSecret;
    private final String baseUrl;
    private final String tokenUrl;

    private volatile SpotifyToken token;

    // Inject ObjectMapper in the constructor
    public SpotifyApiClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${spotify.api.client-id}") String clientId,
            @Value("${spotify.api.client-secret}") String clientSecret,
            @Value("${spotify.api.base-url}") String baseUrl,
            @Value("${spotify.api.token-url}") String tokenUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.baseUrl = baseUrl;
        this.tokenUrl = tokenUrl;
    }

    // ... (getTrackByIsrc, getAlbumById, makeApiCall, and getAccessToken methods remain the same) ...

    /**
     * This method is now more robust. It checks the response from Spotify before
     * attempting to map it, providing much clearer errors on failure.
     */
    private SpotifyToken fetchNewAccessToken() {
        String encodedCredentials = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Basic " + encodedCredentials);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            // Fetch as a generic JsonNode first to inspect the response
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(tokenUrl, request, JsonNode.class);

            JsonNode responseBody = response.getBody();
            if (response.getStatusCode().is2xxSuccessful() && responseBody != null) {
                // Check for an error field, even in a 200 response
                if (responseBody.has("error")) {
                    String error = responseBody.get("error_description").asText("Unknown Spotify authentication error");
                    throw new SpotifyApiException("Spotify returned an error: " + error);
                }

                // If no error, map JsonNode to our SpotifyToken object
                SpotifyToken newToken = objectMapper.treeToValue(responseBody, SpotifyToken.class);
                newToken.calculateExpiryTime();
                log.info("Successfully fetched new Spotify access token.");
                return newToken;
            } else {
                throw new SpotifyApiException("Failed to fetch access token. Status: " + response.getStatusCode());
            }
        } catch (RestClientException | com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("Error during Spotify access token request: {}", e.getMessage());
            throw new SpotifyApiException("Could not fetch access token from Spotify.", e);
        }
    }

    // ... (getTrackByIsrc, getAlbumById, makeApiCall, getAccessToken etc. are unchanged)

    @Cacheable("spotifyTracks")
    public TrackItem getTrackByIsrc(String isrc) {
        log.debug("Calling Spotify API for ISRC: {}", isrc);
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/search")
                .queryParam("type", "track")
                .queryParam("q", "isrc:" + isrc)
                .toUriString();

        SpotifyTrackResponse response = makeApiCall(url, SpotifyTrackResponse.class);

        if (response == null || response.tracks() == null || response.tracks().items().isEmpty()) {
            throw new SpotifyApiException("No track found on Spotify for ISRC: " + isrc);
        }
        return response.tracks().items().get(0);
    }

    @Cacheable("spotifyAlbums")
    public Album getAlbumById(String albumId) {
        log.debug("Calling Spotify API for Album ID: {}", albumId);
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl).path("/albums/{id}").buildAndExpand(albumId).toUriString();
        return makeApiCall(url, Album.class);
    }

    private <T> T makeApiCall(String url, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAccessToken());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.GET, entity, responseType);
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Error calling Spotify API at URL [{}]: {}", url, e.getMessage());
            throw new SpotifyApiException("Failed to retrieve data from Spotify API.", e);
        }
    }

    private String getAccessToken() {
        if (token == null || token.isExpired()) {
            synchronized (this) {
                if (token == null || token.isExpired()) {
                    log.info("Access token is null or expired. Requesting a new one.");
                    this.token = fetchNewAccessToken();
                }
            }
        }
        return this.token.getAccessToken();
    }
}