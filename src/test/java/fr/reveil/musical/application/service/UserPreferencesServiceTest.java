package fr.reveil.musical.application.service;

import fr.reveil.musical.domain.MusicCondition;
import fr.reveil.musical.domain.NotificationChannel;
import fr.reveil.musical.domain.UserId;
import fr.reveil.musical.domain.UserWakeUpPreferences;
import fr.reveil.musical.domain.WeatherType;
import fr.reveil.musical.infrastructure.user.InMemoryUserAccountRepository;
import fr.reveil.musical.infrastructure.user.InMemoryUserPreferencesProvider;
import fr.reveil.musical.application.port.UserAccountRepository;
import fr.reveil.musical.domain.UserAccount;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserPreferencesServiceTest {

    private final UserAccountRepository accounts = new InMemoryUserAccountRepository();
    private final InMemoryUserPreferencesProvider preferencesProvider = new InMemoryUserPreferencesProvider();
    private final UserPreferencesService service = new UserPreferencesService(accounts, preferencesProvider);

    @Test
    void storesAndRetrievesPreferencesForAnExistingUser() {
        UserId userId = new UserId("user-1");
        accounts.save(new UserAccount(userId, "Camille"));
        UserWakeUpPreferences preferences = new UserWakeUpPreferences(
                Map.of(new MusicCondition(DayOfWeek.MONDAY, WeatherType.SOLEIL),
                        java.util.List.of("Song one", "Song two")),
                "Fallback song",
                NotificationChannel.PUSH,
                LocalTime.of(7, 15));

        service.save(userId, preferences);

        assertEquals(preferences, service.get(userId));
    }

    @Test
    void rejectsSavingPreferencesForUnknownUser() {
        UserId unknownUser = new UserId("unknown");
        UserWakeUpPreferences preferences = new UserWakeUpPreferences(
                Map.of(),
                "Fallback song",
                NotificationChannel.EMAIL,
                LocalTime.of(7, 0));

        assertThrows(NoSuchElementException.class, () -> service.save(unknownUser, preferences));
    }

    @Test
    void reportsMissingPreferencesExplicitly() {
        assertThrows(NoSuchElementException.class, () -> service.get(new UserId("user-without-preferences")));
    }
}
