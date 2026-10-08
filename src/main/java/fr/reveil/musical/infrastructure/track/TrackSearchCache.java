package fr.reveil.musical.infrastructure.track;

import fr.reveil.musical.domain.Track;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

@Component
public class TrackSearchCache {

    private static final int MAX_ENTRIES = 1_000;
    private static final Duration FOUND_TTL = Duration.ofHours(24);
    private static final Duration NOT_FOUND_TTL = Duration.ofMinutes(5);

    private final ConcurrentMap<Key, Entry> entries = new ConcurrentHashMap<>();

    public Optional<Track> get(String provider, String query, Supplier<Optional<Track>> search) {
        Key key = new Key(provider, query.strip().toLowerCase(Locale.ROOT));
        Instant now = Instant.now();
        Entry entry = entries.compute(key, (ignored, cached) -> {
            if (cached != null && cached.expiresAt().isAfter(now)) {
                return cached;
            }

            Optional<Track> result = search.get();
            Duration ttl = result.isPresent() ? FOUND_TTL : NOT_FOUND_TTL;
            return new Entry(result, now.plus(ttl));
        });
        evictOverflow();
        return entry.result();
    }

    private void evictOverflow() {
        if (entries.size() <= MAX_ENTRIES) {
            return;
        }

        Instant now = Instant.now();
        entries.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));

        int overflow = entries.size() - MAX_ENTRIES;
        if (overflow <= 0) {
            return;
        }

        var iterator = entries.keySet().iterator();
        while (overflow > 0 && iterator.hasNext()) {
            entries.remove(iterator.next());
            overflow--;
        }
    }

    private record Key(String provider, String query) {
    }

    private record Entry(Optional<Track> result, Instant expiresAt) {
    }
}
