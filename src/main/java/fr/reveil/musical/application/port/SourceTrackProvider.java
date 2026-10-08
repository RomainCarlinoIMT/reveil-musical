package fr.reveil.musical.application.port;

import fr.reveil.musical.domain.MusicSource;

public interface SourceTrackProvider extends TrackProvider {

    MusicSource source();
}
