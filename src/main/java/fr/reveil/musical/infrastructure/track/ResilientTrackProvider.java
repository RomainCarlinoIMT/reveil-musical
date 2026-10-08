package fr.reveil.musical.infrastructure.track;

import fr.reveil.musical.application.port.PreferredTrackProvider;
import fr.reveil.musical.application.port.SourceTrackProvider;
import fr.reveil.musical.application.port.TrackProvider;
import fr.reveil.musical.domain.MusicSourceId;
import fr.reveil.musical.domain.Track;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Primary
@Component("resilientTrackProvider")
public class ResilientTrackProvider implements PreferredTrackProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResilientTrackProvider.class);

    private final List<SourceTrackProvider> remoteProviders;
    private final TrackProvider fallback;

    public ResilientTrackProvider(
            List<SourceTrackProvider> remoteProviders,
            @Qualifier("fallbackTrackProvider") TrackProvider fallback) {
        Map<MusicSourceId, SourceTrackProvider> providersBySource = new HashMap<>();
        for (SourceTrackProvider provider : remoteProviders) {
            MusicSourceId sourceId = Objects.requireNonNull(
                    provider.sourceId(), "provider sourceId must not be null");
            SourceTrackProvider previous = providersBySource.putIfAbsent(sourceId, provider);
            if (previous != null) {
                throw new IllegalArgumentException(
                        "More than one track provider configured for " + sourceId.value());
            }
        }
        this.remoteProviders = List.copyOf(remoteProviders);
        this.fallback = fallback;
    }

    @Override
    public Optional<Track> findTrack(String title) {
        return findTrack(title, Optional.empty());
    }

    @Override
    public Optional<Track> findTrack(String title, Optional<MusicSourceId> preferredSource) {
        validateTitle(title);
        Objects.requireNonNull(preferredSource, "preferredSource must not be null");

        List<SourceTrackProvider> orderedProviders = preferredSource
                .map(source -> remoteProviders.stream()
                        .sorted((first, second) -> Boolean.compare(
                                !first.sourceId().equals(source),
                                !second.sourceId().equals(source)))
                        .toList())
                .orElse(remoteProviders);
        return searchProviders(title, orderedProviders);
    }

    private Optional<Track> searchProviders(String title, List<SourceTrackProvider> providers) {
        for (SourceTrackProvider provider : providers) {
            Optional<Track> track = searchRemote(provider, title);
            if (track.isPresent()) {
                return track;
            }
        }
        LOGGER.warn("Remote track providers returned no track; using the local fallback");
        return fallback.findTrack(title);
    }

    private void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Track title must not be blank");
        }
    }

    private Optional<Track> searchRemote(SourceTrackProvider provider, String title) {
        try {
            return provider.findTrack(title);
        } catch (RuntimeException exception) {
            LOGGER.warn("Track provider {} failed; trying the next source", provider.sourceId().value(), exception);
            return Optional.empty();
        }
    }
}
