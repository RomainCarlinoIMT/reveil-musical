package fr.reveil.musical.domain;

import java.util.Objects;

public record Track(String title, String artist) {

    public Track {
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(artist, "artist must not be null");
        if (title.isBlank() || artist.isBlank()) {
            throw new IllegalArgumentException("title and artist must not be blank");
        }
    }
}
