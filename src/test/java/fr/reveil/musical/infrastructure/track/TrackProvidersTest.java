package fr.reveil.musical.infrastructure.track;

import com.sun.net.httpserver.HttpServer;
import fr.reveil.musical.application.port.TrackProviderException;
import fr.reveil.musical.domain.Track;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrackProvidersTest {

    private HttpServer server;
    private URI baseUri;
    private boolean serverStarted;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        baseUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
    }

    @AfterEach
    void stopServer() {
        if (serverStarted) {
            server.stop(0);
        }
    }

    @Test
    void itunesMapsProviderFieldsAndCachesTheSearch() {
        AtomicInteger requestCount = new AtomicInteger();
        startResponseServer(200, """
                {"results":[{"trackName":"Song title","artistName":"Artist","trackViewUrl":"provider-only"}]}
                """, requestCount, null);
        ItunesTrackProvider provider = new ItunesTrackProvider(
                HttpClient.newHttpClient(),
                JsonMapper.builder().build(),
                new TrackSearchCache(),
                new TrackSearchRateLimiter(),
                baseUri.resolve("/search"));

        assertEquals(Optional.of(new Track("Song title", "Artist")), provider.findTrack("Song title"));
        assertEquals(Optional.of(new Track("Song title", "Artist")), provider.findTrack("SONG TITLE"));
        assertEquals(1, requestCount.get());
    }

    @Test
    void musicBrainzMapsArtistCreditsAndSendsConfiguredUserAgent() {
        AtomicReference<String> userAgent = new AtomicReference<>();
        startResponseServer(200, """
                {"recordings":[{"title":"Song title","artist-credit":[
                  {"artist":{"name":"First artist"},"joinphrase":" & "},
                  {"artist":{"name":"Second artist"}}
                ]}]}
                """, new AtomicInteger(), userAgent);
        MusicBrainzTrackProvider provider = new MusicBrainzTrackProvider(
                HttpClient.newHttpClient(),
                JsonMapper.builder().build(),
                new TrackSearchCache(),
                new TrackSearchRateLimiter(),
                baseUri.resolve("/recording"),
                "ReveilMusical/1.0 (contact: test@example.org)");

        assertEquals(Optional.of(new Track("Song title", "First artist & Second artist")),
                provider.findTrack("Song title"));
        assertEquals("ReveilMusical/1.0 (contact: test@example.org)", userAgent.get());
    }

    @Test
    void httpErrorsAreReportedInsteadOfBeingTreatedAsNoResult() {
        startResponseServer(503, "unavailable", new AtomicInteger(), null);
        ItunesTrackProvider provider = new ItunesTrackProvider(
                HttpClient.newHttpClient(),
                JsonMapper.builder().build(),
                new TrackSearchCache(),
                new TrackSearchRateLimiter(),
                baseUri.resolve("/search"));

        TrackProviderException exception = assertThrows(
                TrackProviderException.class,
                () -> provider.findTrack("Song title"));
        assertTrue(exception.getMessage().contains("HTTP 503"));
    }

    @Test
    void fallbackReturnsKnownLocalTracksWithoutCallingAnExternalProvider() {
        FallbackTrackProvider provider = new FallbackTrackProvider();

        assertEquals(Optional.of(new Track("Aube tranquille", "Réveil Musical")),
                provider.findTrack(" AUBE TRANQUILLE "));
        assertTrue(provider.findTrack("Unknown title").isPresent());
    }

    @Test
    void resilientProviderUsesTheHardcodedFallbackWhenRemoteProvidersFail() {
        FallbackTrackProvider fallback = new FallbackTrackProvider();
        ResilientTrackProvider provider = new ResilientTrackProvider(
                title -> {
                    throw new TrackProviderException("iTunes", "Unavailable");
                },
                title -> {
                    throw new TrackProviderException("MusicBrainz", "Unavailable");
                },
                fallback);

        assertEquals(Optional.of(new Track("Aube tranquille", "Réveil Musical")),
                provider.findTrack("Aube tranquille"));
    }

    @Test
    void rateLimiterRejectsCallsThatExceedTheConfiguredInterval() {
        TrackSearchRateLimiter rateLimiter = new TrackSearchRateLimiter();

        rateLimiter.check("test-provider", Duration.ofSeconds(1));
        assertThrows(TrackProviderException.class,
                () -> rateLimiter.check("test-provider", Duration.ofSeconds(1)));
    }

    private void startResponseServer(
            int status,
            String responseBody,
            AtomicInteger requestCount,
            AtomicReference<String> userAgent) {
        server.createContext("/", exchange -> {
            requestCount.incrementAndGet();
            if (userAgent != null) {
                userAgent.set(exchange.getRequestHeaders().getFirst("User-Agent"));
            }
            byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        serverStarted = true;
    }
}
