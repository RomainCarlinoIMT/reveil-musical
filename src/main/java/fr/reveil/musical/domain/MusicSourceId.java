package fr.reveil.musical.domain;

import java.util.Objects;

public record MusicSourceId(String value) {

    public MusicSourceId {
        Objects.requireNonNull(value, "value must not be null");
        value = value.strip();
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }
}
