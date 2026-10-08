package fr.reveil.musical.application.service;

import fr.reveil.musical.application.port.NotificationAdapter;
import fr.reveil.musical.application.port.PreferredTrackProvider;
import fr.reveil.musical.application.port.UserMusicPreferencesProvider;
import fr.reveil.musical.application.port.UserPreferencesProvider;
import fr.reveil.musical.application.port.WakeUpService;
import fr.reveil.musical.domain.MusicCondition;
import fr.reveil.musical.domain.MusicSourceId;
import fr.reveil.musical.domain.NotificationChannel;
import fr.reveil.musical.domain.Track;
import fr.reveil.musical.domain.UserMusicPreferences;
import fr.reveil.musical.domain.UserWakeUpPreferences;
import fr.reveil.musical.domain.WakeUpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class WakeUpApplicationService implements WakeUpService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WakeUpApplicationService.class);
    private static final UserWakeUpPreferences DEFAULT_WAKE_UP_PREFERENCES =
            UserPreferencesService.DEFAULT_PREFERENCES;
    private static final String DEFAULT_FALLBACK_TRACK = "Aube tranquille";

    private final UserPreferencesProvider userPreferencesProvider;
    private final UserMusicPreferencesProvider userMusicPreferencesProvider;
    private final PreferredTrackProvider trackProvider;
    private final List<NotificationAdapter> notificationAdapters;

    public WakeUpApplicationService(
            UserPreferencesProvider userPreferencesProvider,
            UserMusicPreferencesProvider userMusicPreferencesProvider,
            PreferredTrackProvider trackProvider,
            List<NotificationAdapter> notificationAdapters) {
        this.userPreferencesProvider = userPreferencesProvider;
        this.userMusicPreferencesProvider = userMusicPreferencesProvider;
        this.trackProvider = trackProvider;
        this.notificationAdapters = List.copyOf(notificationAdapters);
    }

    @Override
    public void wakeUp(WakeUpRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        UserWakeUpPreferences preferences = userPreferencesProvider.findPreferences(request.userId())
                .orElseGet(() -> {
                    LOGGER.info("No wake-up preferences found for user {}; using defaults", request.userId().value());
                    return DEFAULT_WAKE_UP_PREFERENCES;
                });
        Optional<UserMusicPreferences> storedMusicPreferences =
                userMusicPreferencesProvider.findByUserId(request.userId());
        if (storedMusicPreferences.isEmpty()) {
            LOGGER.info("No music preferences found for user {}; using the default fallback track",
                    request.userId().value());
        }

        MusicCondition condition = new MusicCondition(request.day(), request.weather());
        Map<MusicCondition, List<String>> tracksByCondition = storedMusicPreferences
                .map(UserMusicPreferences::tracksByCondition)
                .orElseGet(Map::of);
        String fallbackTitle = storedMusicPreferences
                .map(UserMusicPreferences::fallbackTrack)
                .orElse(DEFAULT_FALLBACK_TRACK);
        Optional<MusicSourceId> preferredSource = storedMusicPreferences.map(UserMusicPreferences::preferredSource);
        List<String> preferredTracks = tracksByCondition.get(condition);
        String selectedTitle = preferredTracks == null
                ? fallbackTitle
                : preferredTracks.get(ThreadLocalRandom.current().nextInt(preferredTracks.size()));

        Track track = resolveTrack(selectedTitle, fallbackTitle, preferredSource);
        NotificationAdapter adapter = resolveAdapter(preferences.notificationChannel());
        if (adapter == null) {
            LOGGER.error("No notification adapters are configured; wake-up message was not sent");
            return;
        }
        try {
            adapter.send(request.userId(), track);
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to send wake-up message through {}; trying the default EMAIL channel",
                    adapter.channel(), exception);
            NotificationAdapter fallbackAdapter = findDefaultChannelAdapter(adapter);
            if (fallbackAdapter == null) {
                return;
            }
            try {
                fallbackAdapter.send(request.userId(), track);
            } catch (RuntimeException fallbackException) {
                LOGGER.error("Failed to send wake-up message through the default EMAIL channel",
                        fallbackException);
            }
        }
    }

    private Track resolveTrack(
            String selectedTitle,
            String fallbackTitle,
            Optional<MusicSourceId> preferredSource) {
        return findTrack(selectedTitle, preferredSource)
                .or(() -> selectedTitle.equals(fallbackTitle)
                        ? java.util.Optional.empty()
                        : findTrack(fallbackTitle, preferredSource))
                .orElseThrow(() -> new IllegalStateException(
                        "No track could be resolved for selected or fallback title"));
    }

    private Optional<Track> findTrack(String title, Optional<MusicSourceId> preferredSource) {
        return trackProvider.findTrack(title, preferredSource);
    }

    private NotificationAdapter resolveAdapter(NotificationChannel preferredChannel) {
        Optional<NotificationAdapter> preferredAdapter = notificationAdapters.stream()
                .filter(candidate -> candidate.channel() == preferredChannel)
                .findFirst();
        if (preferredAdapter.isPresent()) {
            return preferredAdapter.get();
        }

        LOGGER.warn("No notification adapter for {}; trying the default EMAIL channel", preferredChannel);
        return notificationAdapters.stream()
                .filter(candidate -> candidate.channel() == NotificationChannel.EMAIL)
                .findFirst()
                .or(() -> notificationAdapters.stream().findFirst())
                .orElse(null);
    }

    private NotificationAdapter findDefaultChannelAdapter(NotificationAdapter failedAdapter) {
        return notificationAdapters.stream()
                .filter(candidate -> candidate != failedAdapter)
                .filter(candidate -> candidate.channel() == NotificationChannel.EMAIL)
                .findFirst()
                .orElse(null);
    }
}
