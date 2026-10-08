package fr.reveil.musical.api.music;

import fr.reveil.musical.domain.MusicCondition;
import fr.reveil.musical.domain.MusicSourceId;
import fr.reveil.musical.domain.UserMusicPreferences;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record SaveMusicPreferencesRequest(
        String preferredSource,
        String fallbackTrack,
        List<MusicConditionRequest> conditions) {

    public UserMusicPreferences toDomain() {
        if (preferredSource == null || preferredSource.isBlank()) {
            throw new IllegalArgumentException("preferredSource must not be blank");
        }
        if (fallbackTrack == null || fallbackTrack.isBlank()) {
            throw new IllegalArgumentException("fallbackTrack must not be blank");
        }
        if (conditions == null) {
            throw new IllegalArgumentException("conditions must not be null");
        }

        Map<MusicCondition, List<String>> tracksByCondition = new HashMap<>();
        for (MusicConditionRequest condition : conditions) {
            if (condition == null) {
                throw new IllegalArgumentException("condition must not be null");
            }
            MusicCondition musicCondition = condition.toDomain();
            if (tracksByCondition.putIfAbsent(musicCondition, condition.tracks()) != null) {
                throw new IllegalArgumentException("condition must not be repeated: " + musicCondition);
            }
        }

        return new UserMusicPreferences(tracksByCondition, fallbackTrack, new MusicSourceId(preferredSource));
    }
}
