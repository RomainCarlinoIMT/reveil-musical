package fr.reveil.musical.api.music;

import fr.reveil.musical.domain.MusicCondition;
import fr.reveil.musical.domain.WeatherType;

import java.time.DayOfWeek;
import java.util.List;

public record MusicConditionRequest(DayOfWeek day, WeatherType weather, List<String> tracks) {

    public MusicCondition toDomain() {
        if (day == null || weather == null) {
            throw new IllegalArgumentException("day and weather must both be provided");
        }
        if (tracks == null || tracks.isEmpty()) {
            throw new IllegalArgumentException("each condition must contain at least one track");
        }
        if (tracks.stream().anyMatch(track -> track == null || track.isBlank())) {
            throw new IllegalArgumentException("track titles must not be blank");
        }
        return new MusicCondition(day, weather);
    }
}
