package fr.reveil.musical.application.service;

import fr.reveil.musical.application.port.NotificationAdapter;
import fr.reveil.musical.application.port.PreferredTrackProvider;
import fr.reveil.musical.application.port.UserMusicPreferencesProvider;
import fr.reveil.musical.application.port.UserPreferencesProvider;
import fr.reveil.musical.domain.MusicCondition;
import fr.reveil.musical.domain.MusicSourceId;
import fr.reveil.musical.domain.NotificationChannel;
import fr.reveil.musical.domain.Track;
import fr.reveil.musical.domain.UserId;
import fr.reveil.musical.domain.UserMusicPreferences;
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
        UserMusicPreferences musicPreferences = musicPreferences(
                Map.of(new MusicCondition(DayOfWeek.MONDAY, WeatherType.SOLEIL), titles),
                "Fallback song",
                new MusicSourceId("musicbrainz"));
        UserWakeUpPreferences wakeUpPreferences = wakeUpPreferences(NotificationChannel.SMS);
        AtomicReference<String> searchedTitle = new AtomicReference<>();
        AtomicReference<Track> sentTrack = new AtomicReference<>();
        AtomicReference<NotificationChannel> sentChannel = new AtomicReference<>();

        AtomicReference<MusicSourceId> searchedSource = new AtomicReference<>();
        PreferredTrackProvider trackProvider = (title, source) -> {
            searchedTitle.set(title);
            searchedSource.set(source.orElse(null));
            return Optional.of(new Track(title, "Artist"));
        };
        NotificationAdapter emailAdapter = notificationAdapter(NotificationChannel.EMAIL, sentChannel, sentTrack);
        NotificationAdapter smsAdapter = notificationAdapter(NotificationChannel.SMS, sentChannel, sentTrack);
        NotificationAdapter pushAdapter = notificationAdapter(NotificationChannel.PUSH, sentChannel, sentTrack);

        WakeUpApplicationService service = new WakeUpApplicationService(
                preferenceProvider(wakeUpPreferences),
                musicPreferenceProvider(musicPreferences),
                trackProvider,
                List.of(emailAdapter, smsAdapter, pushAdapter));

        service.wakeUp(REQUEST);

        assertTrue(titles.contains(searchedTitle.get()));
        assertEquals(NotificationChannel.SMS, sentChannel.get());
        assertEquals(searchedTitle.get(), sentTrack.get().title());
        assertEquals(new MusicSourceId("musicbrainz"), searchedSource.get());
    }

    @Test
    void usesTheFallbackWhenNoTrackListExistsForTheCondition() {
        UserMusicPreferences musicPreferences = musicPreferences(Map.of(), "Fallback song");
        UserWakeUpPreferences wakeUpPreferences = wakeUpPreferences(NotificationChannel.PUSH);
        AtomicReference<String> searchedTitle = new AtomicReference<>();
        AtomicReference<Track> sentTrack = new AtomicReference<>();
        AtomicReference<NotificationChannel> sentChannel = new AtomicReference<>();
        PreferredTrackProvider trackProvider = (title, source) -> {
            searchedTitle.set(title);
            return Optional.of(new Track(title, "Artist"));
        };
        NotificationAdapter pushAdapter = notificationAdapter(
                NotificationChannel.PUSH, sentChannel, sentTrack);

        WakeUpApplicationService service = new WakeUpApplicationService(
                preferenceProvider(wakeUpPreferences),
                musicPreferenceProvider(musicPreferences),
                trackProvider,
                List.of(pushAdapter));

        service.wakeUp(REQUEST);

        assertEquals("Fallback song", searchedTitle.get());
        assertEquals(NotificationChannel.PUSH, sentChannel.get());
        assertEquals("Fallback song", sentTrack.get().title());
    }

    @Test
    void retriesWithFallbackWhenTheSelectedTrackCannotBeResolved() {
        UserMusicPreferences musicPreferences = musicPreferences(
                Map.of(new MusicCondition(DayOfWeek.MONDAY, WeatherType.SOLEIL), List.of("Unknown song")),
                "Fallback song");
        UserWakeUpPreferences wakeUpPreferences = wakeUpPreferences(NotificationChannel.EMAIL);
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
        PreferredTrackProvider trackProvider = (title, source) -> title.equals("Fallback song")
                ? Optional.of(new Track(title, "Artist"))
                : Optional.empty();
        WakeUpApplicationService service = new WakeUpApplicationService(
                preferenceProvider(wakeUpPreferences),
                musicPreferenceProvider(musicPreferences),
                trackProvider,
                List.of(emailAdapter));

        service.wakeUp(REQUEST);

        assertEquals("Fallback song", sentTitle.get());
    }

    @Test
    void usesDefaultPreferencesWhenNoneAreStored() {
        AtomicReference<NotificationChannel> sentChannel = new AtomicReference<>();
        AtomicReference<String> searchedTitle = new AtomicReference<>();
        PreferredTrackProvider trackProvider = (title, preferredSource) -> {
            searchedTitle.set(title);
            assertTrue(preferredSource.isEmpty());
            return Optional.of(new Track(title, "Réveil Musical"));
        };
        NotificationAdapter emailAdapter = notificationAdapter(
                NotificationChannel.EMAIL, sentChannel, new AtomicReference<>());
        WakeUpApplicationService service = new WakeUpApplicationService(
                emptyPreferenceProvider(),
                emptyMusicPreferenceProvider(),
                trackProvider,
                List.of(emailAdapter));

        service.wakeUp(REQUEST);

        assertEquals("Aube tranquille", searchedTitle.get());
        assertEquals(NotificationChannel.EMAIL, sentChannel.get());
    }

    @Test
    void retriesWithDefaultEmailWhenThePreferredChannelSendFails() {
        AtomicReference<NotificationChannel> sentChannel = new AtomicReference<>();
        NotificationAdapter failingSmsAdapter = new NotificationAdapter() {
            @Override
            public NotificationChannel channel() {
                return NotificationChannel.SMS;
            }

            @Override
            public void send(UserId userId, Track track) {
                throw new IllegalStateException("Notification service unavailable");
            }
        };
        NotificationAdapter emailAdapter = notificationAdapter(
                NotificationChannel.EMAIL, sentChannel, new AtomicReference<>());
        PreferredTrackProvider trackProvider = (title, preferredSource) ->
                Optional.of(new Track(title, "Artist"));
        WakeUpApplicationService service = new WakeUpApplicationService(
                preferenceProvider(wakeUpPreferences(NotificationChannel.SMS)),
                musicPreferenceProvider(musicPreferences(Map.of(), "Fallback song")),
                trackProvider,
                List.of(failingSmsAdapter, emailAdapter));

        service.wakeUp(REQUEST);

        assertEquals(NotificationChannel.EMAIL, sentChannel.get());
    }

    @Test
    void logsAndDoesNotPropagateWhenTheDefaultEmailChannelAlsoFails() {
        NotificationAdapter failingEmailAdapter = new NotificationAdapter() {
            @Override
            public NotificationChannel channel() {
                return NotificationChannel.EMAIL;
            }

            @Override
            public void send(UserId userId, Track track) {
                throw new IllegalStateException("Notification service unavailable");
            }
        };
        PreferredTrackProvider trackProvider = (title, preferredSource) ->
                Optional.of(new Track(title, "Artist"));
        WakeUpApplicationService service = new WakeUpApplicationService(
                preferenceProvider(wakeUpPreferences(NotificationChannel.EMAIL)),
                musicPreferenceProvider(musicPreferences(Map.of(), "Fallback song")),
                trackProvider,
                List.of(failingEmailAdapter));

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> service.wakeUp(REQUEST));
    }

    @Test
    void usesDefaultEmailChannelWhenThePreferredChannelAdapterIsUnavailable() {
        AtomicReference<NotificationChannel> sentChannel = new AtomicReference<>();
        NotificationAdapter emailAdapter = notificationAdapter(
                NotificationChannel.EMAIL, sentChannel, new AtomicReference<>());
        PreferredTrackProvider trackProvider = (title, preferredSource) ->
                Optional.of(new Track(title, "Artist"));
        WakeUpApplicationService service = new WakeUpApplicationService(
                preferenceProvider(wakeUpPreferences(NotificationChannel.PUSH)),
                musicPreferenceProvider(musicPreferences(Map.of(), "Fallback song")),
                trackProvider,
                List.of(emailAdapter));

        service.wakeUp(REQUEST);

        assertEquals(NotificationChannel.EMAIL, sentChannel.get());
    }

    private UserWakeUpPreferences wakeUpPreferences(NotificationChannel channel) {
        return new UserWakeUpPreferences(channel, LocalTime.of(7, 30));
    }

    private UserMusicPreferences musicPreferences(
            Map<MusicCondition, List<String>> tracksByCondition,
            String fallback) {
        return musicPreferences(tracksByCondition, fallback, new MusicSourceId("itunes"));
    }

    private UserMusicPreferences musicPreferences(
            Map<MusicCondition, List<String>> tracksByCondition,
            String fallback,
            MusicSourceId preferredSource) {
        return new UserMusicPreferences(tracksByCondition, fallback, preferredSource);
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

    private UserPreferencesProvider emptyPreferenceProvider() {
        return new UserPreferencesProvider() {
            @Override
            public void savePreferences(UserId userId, UserWakeUpPreferences userPreferences) {
                throw new UnsupportedOperationException("Not used by this test");
            }

            @Override
            public Optional<UserWakeUpPreferences> findPreferences(UserId userId) {
                return Optional.empty();
            }
        };
    }

    private UserMusicPreferencesProvider musicPreferenceProvider(UserMusicPreferences preferences) {
        return new UserMusicPreferencesProvider() {
            @Override
            public void save(UserId userId, UserMusicPreferences userPreferences) {
                throw new UnsupportedOperationException("Not used by this test");
            }

            @Override
            public Optional<UserMusicPreferences> findByUserId(UserId userId) {
                return Optional.of(preferences);
            }
        };
    }

    private UserMusicPreferencesProvider emptyMusicPreferenceProvider() {
        return new UserMusicPreferencesProvider() {
            @Override
            public void save(UserId userId, UserMusicPreferences userPreferences) {
                throw new UnsupportedOperationException("Not used by this test");
            }

            @Override
            public Optional<UserMusicPreferences> findByUserId(UserId userId) {
                return Optional.empty();
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
