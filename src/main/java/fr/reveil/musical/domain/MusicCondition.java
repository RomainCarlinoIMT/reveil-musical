package fr.reveil.musical.domain;

import java.time.DayOfWeek;
import java.util.Objects;

public record MusicCondition(DayOfWeek day, WeatherType weather) {

    public MusicCondition {
        Objects.requireNonNull(day, "day must not be null");
        Objects.requireNonNull(weather, "weather must not be null");
    }
}
