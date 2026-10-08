package fr.reveil.musical.domain;

import java.time.DayOfWeek;
import java.util.Objects;

public record WakeUpRequest(UserId userId, DayOfWeek day, WeatherType weather) {

    public WakeUpRequest {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(day, "day must not be null");
        Objects.requireNonNull(weather, "weather must not be null");
    }
}
