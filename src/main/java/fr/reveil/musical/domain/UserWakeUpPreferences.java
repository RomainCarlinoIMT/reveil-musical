package fr.reveil.musical.domain;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record UserWakeUpPreferences(
        Map<MusicCondition, List<String>> tracksByCondition,
        String fallbackTrack,
        NotificationChannel notificationChannel,
        LocalTime sendTime) {

    public UserWakeUpPreferences {
        Objects.requireNonNull(tracksByCondition, "tracksByCondition must not be null");
        Objects.requireNonNull(fallbackTrack, "fallbackTrack must not be null");
        Objects.requireNonNull(notificationChannel, "notificationChannel must not be null");
        Objects.requireNonNull(sendTime, "sendTime must not be null");
        fallbackTrack = fallbackTrack.strip();
        if (fallbackTrack.isBlank()) {
            throw new IllegalArgumentException("fallbackTrack must not be blank");
        }

        Map<MusicCondition, List<String>> immutableTracks = new java.util.HashMap<>();
        tracksByCondition.forEach((condition, titles) -> {
            Objects.requireNonNull(condition, "music condition must not be null");
            Objects.requireNonNull(titles, "track titles must not be null");
            if (titles.isEmpty()) {
                throw new IllegalArgumentException("track titles must not be empty for " + condition);
            }

            List<String> normalizedTitles = new ArrayList<>(titles.size());
            for (String title : titles) {
                Objects.requireNonNull(title, "track title must not be null");
                String normalizedTitle = title.strip();
                if (normalizedTitle.isBlank()) {
                    throw new IllegalArgumentException("track title must not be blank");
                }
                normalizedTitles.add(normalizedTitle);
            }
            immutableTracks.put(condition, List.copyOf(normalizedTitles));
        });
        tracksByCondition = Map.copyOf(immutableTracks);
    }
}
