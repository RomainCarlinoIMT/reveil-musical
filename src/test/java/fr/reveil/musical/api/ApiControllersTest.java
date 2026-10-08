package fr.reveil.musical.api;

import fr.reveil.musical.application.service.UserMusicPreferencesService;
import fr.reveil.musical.application.port.WakeUpService;
import fr.reveil.musical.domain.NotificationChannel;
import fr.reveil.musical.domain.MusicCondition;
import fr.reveil.musical.domain.MusicSourceId;
import fr.reveil.musical.domain.UserId;
import fr.reveil.musical.domain.WakeUpRequest;
import fr.reveil.musical.domain.WeatherType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.context.TestConfiguration;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ApiControllersTest.WakeUpTestConfiguration.class)
class ApiControllersTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserMusicPreferencesService musicPreferencesService;

    @Autowired
    private RecordingWakeUpService wakeUpService;

    @Test
    void createsUserAndReturnsItsGeneratedId() throws Exception {
        HttpResponse<String> response = post("/api/users", """
                {"pseudonym":"  Camille  "}
                """);

        assertEquals(201, response.statusCode());
        JsonNode body = objectMapper.readTree(response.body());
        String userId = body.path("userId").asString();
        assertEquals("Camille", body.path("pseudonym").asString());
        assertEquals(userId, UUID.fromString(userId).toString());
    }

    @Test
    void savesMusicPreferencesByWeekdayAndWeatherForAnExistingUser() throws Exception {
        JsonNode createdUser = objectMapper.readTree(post("/api/users", """
                {"pseudonym":"Camille"}
                """).body());
        String userId = createdUser.path("userId").asString();

        HttpResponse<String> response = post("/api/users/" + userId + "/music-preferences", """
                {
                  "preferredSource":"musicbrainz",
                  "fallbackTrack":"Fallback song",
                  "conditions":[
                    {
                      "day":"MONDAY",
                      "weather":"SOLEIL",
                      "tracks":["Song one","Song two"]
                    }
                  ]
                }
                """);

        assertEquals(204, response.statusCode());
        var preferences = musicPreferencesService.findByUserId(new UserId(userId)).orElseThrow();
        MusicCondition condition = new MusicCondition(DayOfWeek.MONDAY, WeatherType.SOLEIL);
        assertEquals(List.of("Song one", "Song two"), preferences.tracksByCondition().get(condition));
        assertEquals("Fallback song", preferences.fallbackTrack());
        assertEquals(new MusicSourceId("musicbrainz"), preferences.preferredSource());
    }

    @Test
    void rejectsMusicPreferencesForAnUnknownUser() throws Exception {
        HttpResponse<String> response = post("/api/users/" + UUID.randomUUID() + "/music-preferences", """
                {"preferredSource":"itunes","fallbackTrack":"Fallback song","conditions":[]}
                """);

        assertEquals(404, response.statusCode());
    }

    @Test
    void rejectsDuplicateDayAndWeatherConditions() throws Exception {
        JsonNode createdUser = objectMapper.readTree(post("/api/users", """
                {"pseudonym":"Camille"}
                """).body());
        String userId = createdUser.path("userId").asString();

        HttpResponse<String> response = post("/api/users/" + userId + "/music-preferences", """
                {
                  "preferredSource":"itunes",
                  "fallbackTrack":"Fallback song",
                  "conditions":[
                    {"day":"MONDAY","weather":"SOLEIL","tracks":["Song one"]},
                    {"day":"MONDAY","weather":"SOLEIL","tracks":["Song two"]}
                  ]
                }
                """);

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("condition must not be repeated"));
    }

    @Test
    void rejectsIncompleteMusicConditionWithBadRequest() throws Exception {
        JsonNode createdUser = objectMapper.readTree(post("/api/users", """
                {"pseudonym":"Camille"}
                """).body());
        String userId = createdUser.path("userId").asString();

        HttpResponse<String> response = post("/api/users/" + userId + "/music-preferences", """
                {
                  "preferredSource":"itunes",
                  "fallbackTrack":"Fallback song",
                  "conditions":[{"day":"MONDAY","tracks":["Song one"]}]
                }
                """);

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("day and weather must both be provided"));
    }

    @Test
    void rejectsBlankPseudonymWithBadRequest() throws Exception {
        HttpResponse<String> response = post("/api/users", """
                {"pseudonym":"  "}
                """);

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("pseudonym must not be blank"));
    }

    @Test
    void rejectsMusicPreferencesWithoutPreferredSource() throws Exception {
        JsonNode createdUser = objectMapper.readTree(post("/api/users", """
                {"pseudonym":"Camille"}
                """).body());
        String userId = createdUser.path("userId").asString();

        HttpResponse<String> response = post("/api/users/" + userId + "/music-preferences", """
                {"fallbackTrack":"Fallback song","conditions":[]}
                """);

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("preferredSource must not be blank"));
    }

    @Test
    void returnsAllMusicConfiguredForTheRequestedMorningCondition() throws Exception {
        JsonNode createdUser = objectMapper.readTree(post("/api/users", """
                {"pseudonym":"Camille"}
                """).body());
        String userId = createdUser.path("userId").asString();
        post("/api/users/" + userId + "/music-preferences", """
                {
                  "preferredSource":"itunes",
                  "fallbackTrack":"Fallback song",
                  "conditions":[
                    {"day":"MONDAY","weather":"SOLEIL","tracks":["Song one","Song two"]},
                    {"day":"MONDAY","weather":"PLUIE","tracks":["Rain song"]}
                  ]
                }
                """);

        HttpResponse<String> response = get("/api/users/" + userId
                + "/morning-music?day=MONDAY&weather=SOLEIL");

        assertEquals(200, response.statusCode());
        assertEquals(List.of("Song one", "Song two"),
                objectMapper.readValue(response.body(), objectMapper.getTypeFactory()
                        .constructCollectionType(List.class, String.class)));
    }

    @Test
    void returnsFallbackForMorningWhenNoTracksMatchTheCondition() throws Exception {
        JsonNode createdUser = objectMapper.readTree(post("/api/users", """
                {"pseudonym":"Camille"}
                """).body());
        String userId = createdUser.path("userId").asString();
        post("/api/users/" + userId + "/music-preferences", """
                {"preferredSource":"itunes","fallbackTrack":"Fallback song","conditions":[]}
                """);

        HttpResponse<String> response = get("/api/users/" + userId
                + "/morning-music?day=MONDAY&weather=SOLEIL");

        assertEquals(200, response.statusCode());
        assertEquals(List.of("Fallback song"),
                objectMapper.readValue(response.body(), objectMapper.getTypeFactory()
                        .constructCollectionType(List.class, String.class)));
    }

    @Test
    void rejectsMorningMusicRequestForAnUnknownUser() throws Exception {
        HttpResponse<String> response = get("/api/users/" + UUID.randomUUID()
                + "/morning-music?day=MONDAY&weather=SOLEIL");

        assertEquals(404, response.statusCode());
    }

    @Test
    void rejectsMorningMusicRequestWhenTheUserHasNoMusicPreferences() throws Exception {
        JsonNode createdUser = objectMapper.readTree(post("/api/users", """
                {"pseudonym":"Camille"}
                """).body());
        String userId = createdUser.path("userId").asString();

        HttpResponse<String> response = get("/api/users/" + userId
                + "/morning-music?day=MONDAY&weather=SOLEIL");

        assertEquals(404, response.statusCode());
    }

    @Test
    void rejectsInvalidMorningConditionParameters() throws Exception {
        JsonNode createdUser = objectMapper.readTree(post("/api/users", """
                {"pseudonym":"Camille"}
                """).body());
        String userId = createdUser.path("userId").asString();

        HttpResponse<String> response = get("/api/users/" + userId
                + "/morning-music?day=INVALID&weather=SOLEIL");

        assertEquals(400, response.statusCode());
    }

    @Test
    void readsAndUpdatesNotificationChannelForAnExistingUser() throws Exception {
        JsonNode createdUser = objectMapper.readTree(post("/api/users", """
                {"pseudonym":"Camille"}
                """).body());
        String userId = createdUser.path("userId").asString();

        HttpResponse<String> defaultResponse = get("/api/users/" + userId + "/preferences/channel");
        assertEquals(200, defaultResponse.statusCode());
        assertEquals("EMAIL", objectMapper.readTree(defaultResponse.body()).path("channel").asString());

        HttpResponse<String> updateResponse = put(
                "/api/users/" + userId + "/preferences/channel",
                "{\"channel\":\"PUSH\"}");
        assertEquals(200, updateResponse.statusCode());
        assertEquals(NotificationChannel.PUSH.name(),
                objectMapper.readTree(updateResponse.body()).get("channel").asString());
    }

    @Test
    void rejectsChannelUpdateForAnUnknownUser() throws Exception {
        HttpResponse<String> response = put(
                "/api/users/" + UUID.randomUUID() + "/preferences/channel",
                "{\"channel\":\"PUSH\"}");

        assertEquals(404, response.statusCode());
    }

    @Test
    void rejectsWakeUpRequestForAnUnknownUser() throws Exception {
        HttpResponse<String> response = postWithoutBody(
                "/api/users/" + UUID.randomUUID() + "/wake-up?day=MONDAY&weather=SOLEIL");

        assertEquals(404, response.statusCode());
    }

    @Test
    void triggersWakeUpWithTheProvidedDayAndWeather() throws Exception {
        JsonNode createdUser = objectMapper.readTree(post("/api/users", """
                {"pseudonym":"Camille"}
                """).body());
        String userId = createdUser.path("userId").asString();

        HttpResponse<String> response = postWithoutBody(
                "/api/users/" + userId + "/wake-up?day=MONDAY&weather=SOLEIL");

        assertEquals(204, response.statusCode());
        assertEquals(new WakeUpRequest(
                new UserId(userId), DayOfWeek.MONDAY, WeatherType.SOLEIL), wakeUpService.lastRequest());
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .GET()
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> put(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postWithoutBody(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    @TestConfiguration
    static class WakeUpTestConfiguration {

        @Bean
        @Primary
        RecordingWakeUpService recordingWakeUpService() {
            return new RecordingWakeUpService();
        }
    }

    static class RecordingWakeUpService implements WakeUpService {

        private volatile WakeUpRequest lastRequest;

        @Override
        public void wakeUp(WakeUpRequest request) {
            lastRequest = request;
        }

        WakeUpRequest lastRequest() {
            return lastRequest;
        }
    }
}
