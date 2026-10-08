package fr.reveil.musical.application.port;

import fr.reveil.musical.domain.Track;

import java.util.Optional;

public interface TrackProvider {

    Optional<Track> findTrack(String title);
}
