package fr.reveil.musical.infrastructure.track;

import fr.reveil.musical.application.port.SourceTrackProvider;
import fr.reveil.musical.application.port.TrackProviderException;
import fr.reveil.musical.domain.MusicSourceId;
import fr.reveil.musical.domain.Track;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

@Component("itunesTrackProvider")
public class ItunesTrackProvider implements SourceTrackProvider {

    private static final String PROVIDER = "iTunes";
    private static final MusicSourceId SOURCE_ID = new MusicSourceId("itunes");
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration MINIMUM_REQUEST_INTERVAL = Duration.ofSeconds(3);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI searchUri;
    private final TrackSearchCache cache;
    private final TrackSearchRateLimiter rateLimiter;

    public ItunesTrackProvider(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            TrackSearchCache cache,
            TrackSearchRateLimiter rateLimiter,
            @Value("${tracks.itunes.search-uri:https://itunes.apple.com/search}") URI searchUri) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.cache = cache;
        this.rateLimiter = rateLimiter;
        this.searchUri = searchUri;
    }

    @Override
    public MusicSourceId sourceId() {
        return SOURCE_ID;
    }

    @Override
    public Optional<Track> findTrack(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Track title must not be blank");
        }
        return cache.get(PROVIDER, title, () -> search(title));
    }

    private Optional<Track> search(String title) {
        rateLimiter.check(PROVIDER, MINIMUM_REQUEST_INTERVAL);
        URI requestUri = URI.create(searchUri + "?term="
                + URLEncoder.encode(title, StandardCharsets.UTF_8)
                + "&media=music&limit=5");
        HttpRequest request = HttpRequest.newBuilder(requestUri)
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new TrackProviderException(PROVIDER, "Search request was interrupted", exception);
        } catch (IOException exception) {
            throw new TrackProviderException(PROVIDER, "Search request failed", exception);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new TrackProviderException(PROVIDER, "Search returned HTTP " + response.statusCode());
        }

        try {
            JsonNode root = objectMapper.readTree(response.body());
            if (root == null) {
                throw new TrackProviderException(PROVIDER, "Search response body was empty");
            }
            return mapResponse(root);
        } catch (tools.jackson.core.JacksonException exception) {
            throw new TrackProviderException(PROVIDER, "Search response was not valid JSON", exception);
        }
    }

    private Optional<Track> mapResponse(JsonNode root) {
        JsonNode results = root.path("results");
        if (!results.isArray()) {
            throw new TrackProviderException(PROVIDER, "Search response has no results array");
        }

        for (JsonNode result : results) {
            String title = result.path("trackName").asString("");
            String artist = result.path("artistName").asString("");
            if (!title.isBlank() && !artist.isBlank()) {
                return Optional.of(new Track(title, artist));
            }
        }
        return Optional.empty();
    }
}
