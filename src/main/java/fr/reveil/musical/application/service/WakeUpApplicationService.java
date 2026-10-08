package fr.reveil.musical.application.service;

import fr.reveil.musical.application.port.NotificationAdapter;
import fr.reveil.musical.application.port.TrackProvider;
import fr.reveil.musical.application.port.UserMusicPreferencesProvider;
import fr.reveil.musical.application.port.UserPreferencesProvider;
import fr.reveil.musical.application.port.WakeUpService;
import fr.reveil.musical.domain.MusicCondition;
import fr.reveil.musical.domain.Track;
import fr.reveil.musical.domain.UserMusicPreferences;
import fr.reveil.musical.domain.UserWakeUpPreferences;
import fr.reveil.musical.domain.WakeUpRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class WakeUpApplicationService implements WakeUpService {

    private final UserPreferencesProvider userPreferencesProvider;
    private final UserMusicPreferencesProvider userMusicPreferencesProvider;
    private final TrackProvider trackProvider;
    private final List<NotificationAdapter> notificationAdapters;

    public WakeUpApplicationService(
            UserPreferencesProvider userPreferencesProvider,
            UserMusicPreferencesProvider userMusicPreferencesProvider,
            TrackProvider trackProvider,
            List<NotificationAdapter> notificationAdapters) {
        this.userPreferencesProvider = userPreferencesProvider;
        this.userMusicPreferencesProvider = userMusicPreferencesProvider;
        this.trackProvider = trackProvider;
        this.notificationAdapters = List.copyOf(notificationAdapters);
    }

    @Override
    public void wakeUp(WakeUpRequest request) {
        UserWakeUpPreferences preferences = userPreferencesProvider.findPreferences(request.userId())
                .orElseThrow(() -> new NoSuchElementException(
                        "No wake-up preferences found for user ID " + request.userId().value()));
        UserMusicPreferences musicPreferences = userMusicPreferencesProvider.findByUserId(request.userId())
                .orElseThrow(() -> new NoSuchElementException(
                        "No music preferences found for user ID " + request.userId().value()));

        MusicCondition condition = new MusicCondition(request.day(), request.weather());
        List<String> preferredTracks = musicPreferences.tracksByCondition().get(condition);
        String selectedTitle = preferredTracks == null
                ? musicPreferences.fallbackTrack()
                : preferredTracks.get(ThreadLocalRandom.current().nextInt(preferredTracks.size()));

        Track track = resolveTrack(selectedTitle, musicPreferences.fallbackTrack());
        NotificationAdapter adapter = notificationAdapters.stream()
                .filter(candidate -> candidate.channel() == preferences.notificationChannel())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No notification adapter configured for " + preferences.notificationChannel()));
        adapter.send(request.userId(), track);
    }

    private Track resolveTrack(String selectedTitle, String fallbackTitle) {
        return trackProvider.findTrack(selectedTitle)
                .or(() -> selectedTitle.equals(fallbackTitle)
                        ? java.util.Optional.empty()
                        : trackProvider.findTrack(fallbackTitle))
                .orElseThrow(() -> new IllegalStateException(
                        "No track could be resolved for selected or fallback title"));
    }
}
