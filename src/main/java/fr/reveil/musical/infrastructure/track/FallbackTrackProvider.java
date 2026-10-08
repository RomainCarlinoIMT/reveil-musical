package fr.reveil.musical.infrastructure.track;

import fr.reveil.musical.application.port.TrackProvider;
import fr.reveil.musical.domain.Track;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component("fallbackTrackProvider")
public class FallbackTrackProvider implements TrackProvider {

    private final List<Track> tracks = List.of(
            new Track("Aube tranquille", "Réveil Musical"),
            new Track("Pluie douce", "Réveil Musical"),
            new Track("Neige au matin", "Réveil Musical"));

    @Override
    public Optional<Track> findTrack(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Track title must not be blank");
        }
        String normalizedTitle = title.strip().toLowerCase(Locale.ROOT);
        return tracks.stream()
                .filter(track -> track.title().toLowerCase(Locale.ROOT).equals(normalizedTitle))
                .findFirst()
                .or(() -> Optional.of(tracks.get(Math.floorMod(normalizedTitle.hashCode(), tracks.size()))));
    }
}
