package fr.reveil.musical.application.port;

import fr.reveil.musical.domain.MusicSourceId;
import fr.reveil.musical.domain.Track;

import java.util.Optional;

public interface PreferredTrackProvider extends TrackProvider {

    Optional<Track> findTrack(String title, Optional<MusicSourceId> preferredSource);

    @Override
    default Optional<Track> findTrack(String title) {
        return findTrack(title, Optional.empty());
    }
}
