package fr.reveil.musical.api;

import fr.reveil.musical.application.service.UserMusicPreferencesService;
import fr.reveil.musical.domain.MusicCondition;
import fr.reveil.musical.domain.MusicSource;
import fr.reveil.musical.domain.UserId;
import fr.reveil.musical.domain.WeatherType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
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
class ApiControllersTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserMusicPreferencesService musicPreferencesService;

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
                  "preferredSource":"MUSICBRAINZ",
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
        assertEquals(MusicSource.MUSICBRAINZ, preferences.preferredSource());
    }

    @Test
    void rejectsMusicPreferencesForAnUnknownUser() throws Exception {
        HttpResponse<String> response = post("/api/users/" + UUID.randomUUID() + "/music-preferences", """
                {"preferredSource":"ITUNES","fallbackTrack":"Fallback song","conditions":[]}
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
                  "preferredSource":"ITUNES",
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
                  "preferredSource":"ITUNES",
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
        assertTrue(response.body().contains("preferredSource must be provided"));
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }
}
