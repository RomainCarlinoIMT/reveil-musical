package fr.reveil.musical.infrastructure.track;

import fr.reveil.musical.application.port.SourceTrackProvider;
import fr.reveil.musical.application.port.TrackProviderException;
import fr.reveil.musical.domain.MusicSource;
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
import java.util.StringJoiner;

@Component("musicBrainzTrackProvider")
public class MusicBrainzTrackProvider implements SourceTrackProvider {

    private static final String PROVIDER = "MusicBrainz";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration MINIMUM_REQUEST_INTERVAL = Duration.ofSeconds(1);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final TrackSearchCache cache;
    private final TrackSearchRateLimiter rateLimiter;
    private final URI recordingUri;
    private final String userAgent;

    public MusicBrainzTrackProvider(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            TrackSearchCache cache,
            TrackSearchRateLimiter rateLimiter,
            @Value("${tracks.musicbrainz.recording-uri:https://musicbrainz.org/ws/2/recording}") URI recordingUri,
            @Value("${tracks.musicbrainz.user-agent:}") String userAgent) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.cache = cache;
        this.rateLimiter = rateLimiter;
        this.recordingUri = recordingUri;
        this.userAgent = userAgent;
    }

    @Override
    public MusicSource source() {
        return MusicSource.MUSICBRAINZ;
    }

    @Override
    public Optional<Track> findTrack(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Track title must not be blank");
        }
        if (userAgent.isBlank()) {
            throw new TrackProviderException(PROVIDER,
                    "Configure tracks.musicbrainz.user-agent with an application name and contact before searching");
        }
        return cache.get(PROVIDER, title, () -> search(title));
    }

    private Optional<Track> search(String title) {
        rateLimiter.check(PROVIDER, MINIMUM_REQUEST_INTERVAL);
        URI requestUri = URI.create(recordingUri + "?query="
                + URLEncoder.encode(title, StandardCharsets.UTF_8)
                + "&fmt=json&limit=5");
        HttpRequest request = HttpRequest.newBuilder(requestUri)
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("User-Agent", userAgent)
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
        JsonNode recordings = root.path("recordings");
        if (!recordings.isArray()) {
            throw new TrackProviderException(PROVIDER, "Search response has no recordings array");
        }

        for (JsonNode recording : recordings) {
            String title = recording.path("title").asString("");
            String artist = mapArtistCredit(recording.path("artist-credit"));
            if (!title.isBlank() && !artist.isBlank()) {
                return Optional.of(new Track(title, artist));
            }
        }
        return Optional.empty();
    }

    private String mapArtistCredit(JsonNode artistCredit) {
        if (!artistCredit.isArray()) {
            return "";
        }

        StringJoiner artists = new StringJoiner("");
        for (JsonNode credit : artistCredit) {
            String name = credit.path("artist").path("name").asString("");
            if (name.isBlank()) {
                name = credit.path("name").asString("");
            }
            if (name.isBlank()) {
                continue;
            }

            artists.add(name);
            artists.add(credit.path("joinphrase").asString(""));
        }
        return artists.toString().strip();
    }
}
