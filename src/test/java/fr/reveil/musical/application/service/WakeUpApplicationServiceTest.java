package fr.reveil.musical.application.service;

import fr.reveil.musical.application.port.NotificationAdapter;
import fr.reveil.musical.application.port.TrackProvider;
import fr.reveil.musical.application.port.UserPreferencesProvider;
import fr.reveil.musical.domain.MusicCondition;
import fr.reveil.musical.domain.NotificationChannel;
import fr.reveil.musical.domain.Track;
import fr.reveil.musical.domain.UserId;
import fr.reveil.musical.domain.UserWakeUpPreferences;
import fr.reveil.musical.domain.WakeUpRequest;
import fr.reveil.musical.domain.WeatherType;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WakeUpApplicationServiceTest {

    private static final UserId USER_ID = new UserId("user-1");
    private static final WakeUpRequest REQUEST = new WakeUpRequest(USER_ID, DayOfWeek.MONDAY, WeatherType.SOLEIL);

    @Test
    void selectsFromTheRequestedDayAndWeatherAndUsesThePreferredChannel() {
        List<String> titles = List.of("Song one", "Song two");
        UserWakeUpPreferences preferences = preferences(
                Map.of(new MusicCondition(DayOfWeek.MONDAY, WeatherType.SOLEIL), titles),
                "Fallback song",
                NotificationChannel.SMS);
        AtomicReference<String> searchedTitle = new AtomicReference<>();
        AtomicReference<Track> sentTrack = new AtomicReference<>();
        AtomicReference<NotificationChannel> sentChannel = new AtomicReference<>();

        TrackProvider trackProvider = title -> {
            searchedTitle.set(title);
            return Optional.of(new Track(title, "Artist"));
        };
        NotificationAdapter emailAdapter = notificationAdapter(NotificationChannel.EMAIL, sentChannel, sentTrack);
        NotificationAdapter smsAdapter = notificationAdapter(NotificationChannel.SMS, sentChannel, sentTrack);
        NotificationAdapter pushAdapter = notificationAdapter(NotificationChannel.PUSH, sentChannel, sentTrack);

        WakeUpApplicationService service = new WakeUpApplicationService(
                preferenceProvider(preferences),
                trackProvider,
                List.of(emailAdapter, smsAdapter, pushAdapter));

        service.wakeUp(REQUEST);

        assertTrue(titles.contains(searchedTitle.get()));
        assertEquals(NotificationChannel.SMS, sentChannel.get());
        assertEquals(searchedTitle.get(), sentTrack.get().title());
    }

    @Test
    void usesTheFallbackWhenNoTrackListExistsForTheCondition() {
        UserWakeUpPreferences preferences = preferences(Map.of(), "Fallback song", NotificationChannel.PUSH);
        AtomicReference<String> searchedTitle = new AtomicReference<>();
        AtomicReference<Track> sentTrack = new AtomicReference<>();
        AtomicReference<NotificationChannel> sentChannel = new AtomicReference<>();
        TrackProvider trackProvider = title -> {
            searchedTitle.set(title);
            return Optional.of(new Track(title, "Artist"));
        };
        NotificationAdapter pushAdapter = notificationAdapter(
                NotificationChannel.PUSH, sentChannel, sentTrack);

        WakeUpApplicationService service = new WakeUpApplicationService(
                preferenceProvider(preferences), trackProvider, List.of(pushAdapter));

        service.wakeUp(REQUEST);

        assertEquals("Fallback song", searchedTitle.get());
        assertEquals(NotificationChannel.PUSH, sentChannel.get());
        assertEquals("Fallback song", sentTrack.get().title());
    }

    @Test
    void retriesWithFallbackWhenTheSelectedTrackCannotBeResolved() {
        UserWakeUpPreferences preferences = preferences(
                Map.of(new MusicCondition(DayOfWeek.MONDAY, WeatherType.SOLEIL), List.of("Unknown song")),
                "Fallback song",
                NotificationChannel.EMAIL);
        AtomicReference<String> sentTitle = new AtomicReference<>();
        NotificationAdapter emailAdapter = new NotificationAdapter() {
            @Override
            public NotificationChannel channel() {
                return NotificationChannel.EMAIL;
            }

            @Override
            public void send(UserId userId, Track track) {
                sentTitle.set(track.title());
            }
        };
        TrackProvider trackProvider = title -> title.equals("Fallback song")
                ? Optional.of(new Track(title, "Artist"))
                : Optional.empty();
        WakeUpApplicationService service = new WakeUpApplicationService(
                preferenceProvider(preferences), trackProvider, List.of(emailAdapter));

        service.wakeUp(REQUEST);

        assertEquals("Fallback song", sentTitle.get());
    }

    private UserWakeUpPreferences preferences(
            Map<MusicCondition, List<String>> tracksByCondition,
            String fallback,
            NotificationChannel channel) {
        return new UserWakeUpPreferences(
                tracksByCondition, fallback, channel, LocalTime.of(7, 30));
    }

    private UserPreferencesProvider preferenceProvider(UserWakeUpPreferences preferences) {
        return new UserPreferencesProvider() {
            @Override
            public void savePreferences(UserId userId, UserWakeUpPreferences userPreferences) {
                throw new UnsupportedOperationException("Not used by this test");
            }

            @Override
            public Optional<UserWakeUpPreferences> findPreferences(UserId userId) {
                return Optional.of(preferences);
            }
        };
    }

    private NotificationAdapter notificationAdapter(
            NotificationChannel channel,
            AtomicReference<NotificationChannel> sentChannel,
            AtomicReference<Track> sentTrack) {
        return new NotificationAdapter() {
            @Override
            public NotificationChannel channel() {
                return channel;
            }

            @Override
            public void send(UserId userId, Track track) {
                sentChannel.set(channel);
                sentTrack.set(track);
            }
        };
    }
}
