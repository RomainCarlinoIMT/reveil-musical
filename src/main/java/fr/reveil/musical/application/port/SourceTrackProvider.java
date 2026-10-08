package fr.reveil.musical.application.port;

import fr.reveil.musical.domain.MusicSourceId;

public interface SourceTrackProvider extends TrackProvider {

    MusicSourceId sourceId();
}
