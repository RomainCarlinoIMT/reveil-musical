package fr.reveil.musical.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record UserMusicPreferences(
        Map<MusicCondition, List<String>> tracksByCondition,
        String fallbackTrack,
        MusicSource preferredSource) {

    public UserMusicPreferences {
        Objects.requireNonNull(tracksByCondition, "tracksByCondition must not be null");
        Objects.requireNonNull(fallbackTrack, "fallbackTrack must not be null");
        Objects.requireNonNull(preferredSource, "preferredSource must not be null");
        fallbackTrack = normalizeTitle(fallbackTrack, "fallbackTrack");

        Map<MusicCondition, List<String>> immutableTracks = new HashMap<>();
        tracksByCondition.forEach((condition, titles) -> {
            Objects.requireNonNull(condition, "music condition must not be null");
            Objects.requireNonNull(titles, "track titles must not be null");
            if (titles.isEmpty()) {
                throw new IllegalArgumentException("track titles must not be empty for " + condition);
            }

            List<String> normalizedTitles = new ArrayList<>(titles.size());
            for (String title : titles) {
                normalizedTitles.add(normalizeTitle(title, "track title"));
            }
            immutableTracks.put(condition, List.copyOf(normalizedTitles));
        });
        tracksByCondition = Map.copyOf(immutableTracks);
    }

    private static String normalizeTitle(String title, String fieldName) {
        Objects.requireNonNull(title, fieldName + " must not be null");
        String normalizedTitle = title.strip();
        if (normalizedTitle.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalizedTitle;
    }
}
