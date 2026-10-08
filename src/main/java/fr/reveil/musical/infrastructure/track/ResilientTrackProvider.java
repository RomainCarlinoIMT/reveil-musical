package fr.reveil.musical.infrastructure.track;

import fr.reveil.musical.application.port.PreferredTrackProvider;
import fr.reveil.musical.application.port.SourceTrackProvider;
import fr.reveil.musical.application.port.TrackProvider;
import fr.reveil.musical.application.port.TrackProviderException;
import fr.reveil.musical.domain.MusicSource;
import fr.reveil.musical.domain.Track;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Primary
@Component("resilientTrackProvider")
public class ResilientTrackProvider implements PreferredTrackProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResilientTrackProvider.class);

    private final Map<MusicSource, SourceTrackProvider> remoteProviders;
    private final TrackProvider fallback;

    public ResilientTrackProvider(
            List<SourceTrackProvider> remoteProviders,
            @Qualifier("fallbackTrackProvider") TrackProvider fallback) {
        EnumMap<MusicSource, SourceTrackProvider> providersBySource = new EnumMap<>(MusicSource.class);
        for (SourceTrackProvider provider : remoteProviders) {
            SourceTrackProvider previous = providersBySource.putIfAbsent(provider.source(), provider);
            if (previous != null) {
                throw new IllegalArgumentException("More than one track provider configured for " + provider.source());
            }
        }
        this.remoteProviders = Map.copyOf(providersBySource);
        this.fallback = fallback;
    }

    @Override
    public Optional<Track> findTrack(String title, MusicSource preferredSource) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Track title must not be blank");
        }
        Objects.requireNonNull(preferredSource, "preferredSource must not be null");

        for (MusicSource source : searchOrder(preferredSource)) {
            SourceTrackProvider provider = remoteProviders.get(source);
            if (provider == null) {
                continue;
            }
            Optional<Track> track = searchRemote(provider, title);
            if (track.isPresent()) {
                return track;
            }
        }

        LOGGER.warn("Remote track providers returned no track; using the local fallback");
        return fallback.findTrack(title);
    }

    private List<MusicSource> searchOrder(MusicSource preferredSource) {
        List<MusicSource> order = new ArrayList<>();
        order.add(preferredSource);
        for (MusicSource source : MusicSource.values()) {
            if (source != preferredSource) {
                order.add(source);
            }
        }
        return order;
    }

    private Optional<Track> searchRemote(SourceTrackProvider provider, String title) {
        try {
            return provider.findTrack(title);
        } catch (TrackProviderException exception) {
            LOGGER.warn("Track provider failed; trying the next source", exception);
            return Optional.empty();
        }
    }
}
