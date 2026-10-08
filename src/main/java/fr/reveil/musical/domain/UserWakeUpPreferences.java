package fr.reveil.musical.domain;

import java.util.Map;
import java.util.Objects;

public record UserWakeUpPreferences(
        Map<WeatherType, String> tracksByWeather,
        String fallbackTrack,
        NotificationChannel notificationChannel) {

    public UserWakeUpPreferences {
        Objects.requireNonNull(tracksByWeather, "tracksByWeather must not be null");
        Objects.requireNonNull(fallbackTrack, "fallbackTrack must not be null");
        Objects.requireNonNull(notificationChannel, "notificationChannel must not be null");
        if (fallbackTrack.isBlank()) {
            throw new IllegalArgumentException("fallbackTrack must not be blank");
        }
        tracksByWeather = Map.copyOf(tracksByWeather);
    }
}
