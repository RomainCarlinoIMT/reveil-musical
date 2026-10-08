package fr.reveil.musical.infrastructure.track;

import fr.reveil.musical.application.port.TrackProvider;
import fr.reveil.musical.application.port.TrackProviderException;
import fr.reveil.musical.domain.Track;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Primary
@Component("resilientTrackProvider")
public class ResilientTrackProvider implements TrackProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResilientTrackProvider.class);

    private final TrackProvider itunes;
    private final TrackProvider musicBrainz;
    private final TrackProvider fallback;

    public ResilientTrackProvider(
            @Qualifier("itunesTrackProvider") TrackProvider itunes,
            @Qualifier("musicBrainzTrackProvider") TrackProvider musicBrainz,
            @Qualifier("fallbackTrackProvider") TrackProvider fallback) {
        this.itunes = itunes;
        this.musicBrainz = musicBrainz;
        this.fallback = fallback;
    }

    @Override
    public Optional<Track> findTrack(String title) {
        Optional<Track> track = searchRemote(itunes, title);
        if (track.isPresent()) {
            return track;
        }

        track = searchRemote(musicBrainz, title);
        if (track.isPresent()) {
            return track;
        }

        LOGGER.warn("Remote track providers returned no track; using the local fallback");
        return fallback.findTrack(title);
    }

    private Optional<Track> searchRemote(TrackProvider provider, String title) {
        try {
            return provider.findTrack(title);
        } catch (TrackProviderException exception) {
            LOGGER.warn("Track provider failed; trying the next source", exception);
            return Optional.empty();
        }
    }
}
