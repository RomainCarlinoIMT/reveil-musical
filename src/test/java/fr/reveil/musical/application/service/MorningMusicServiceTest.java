package fr.reveil.musical.application.service;

import fr.reveil.musical.domain.MusicCondition;
import fr.reveil.musical.domain.MusicSource;
import fr.reveil.musical.domain.UserAccount;
import fr.reveil.musical.domain.UserId;
import fr.reveil.musical.domain.UserMusicPreferences;
import fr.reveil.musical.domain.WeatherType;
import fr.reveil.musical.infrastructure.user.InMemoryUserAccountRepository;
import fr.reveil.musical.infrastructure.user.InMemoryUserMusicPreferencesProvider;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MorningMusicServiceTest {

    private static final UserId USER_ID = new UserId("user-1");

    private final InMemoryUserAccountRepository accounts = new InMemoryUserAccountRepository();
    private final InMemoryUserMusicPreferencesProvider preferences = new InMemoryUserMusicPreferencesProvider();
    private final UserMusicPreferencesService preferencesService = new UserMusicPreferencesService(accounts, preferences);
    private final MorningMusicService service = new MorningMusicService(preferencesService);

    @Test
    void returnsTheCompleteListForTheRequestedDayAndWeather() {
        MusicCondition condition = new MusicCondition(DayOfWeek.MONDAY, WeatherType.SOLEIL);
        savePreferences(Map.of(
                condition, List.of("Song one", "Song two"),
                new MusicCondition(DayOfWeek.TUESDAY, WeatherType.PLUIE), List.of("Other song")));

        assertEquals(List.of("Song one", "Song two"),
                service.findMusicFor(USER_ID, DayOfWeek.MONDAY, WeatherType.SOLEIL));
    }

    @Test
    void returnsTheFallbackWhenTheRequestedConditionHasNoTrackList() {
        savePreferences(Map.of(), "Fallback song");

        assertEquals(List.of("Fallback song"),
                service.findMusicFor(USER_ID, DayOfWeek.MONDAY, WeatherType.SOLEIL));
    }

    @Test
    void reportsMissingPreferencesForTheRequestedUser() {
        accounts.save(new UserAccount(USER_ID, "Camille"));

        assertThrows(NoSuchElementException.class,
                () -> service.findMusicFor(USER_ID, DayOfWeek.MONDAY, WeatherType.SOLEIL));
    }

    private void savePreferences(Map<MusicCondition, List<String>> tracksByCondition) {
        savePreferences(tracksByCondition, "Fallback song");
    }

    private void savePreferences(Map<MusicCondition, List<String>> tracksByCondition, String fallbackTrack) {
        accounts.save(new UserAccount(USER_ID, "Camille"));
        preferencesService.save(USER_ID,
                new UserMusicPreferences(tracksByCondition, fallbackTrack, MusicSource.ITUNES));
    }
}
