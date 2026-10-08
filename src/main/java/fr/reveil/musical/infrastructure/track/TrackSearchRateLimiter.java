package fr.reveil.musical.infrastructure.track;

import fr.reveil.musical.application.port.TrackProviderException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class TrackSearchRateLimiter {

    private final ConcurrentMap<String, AtomicLong> lastRequests = new ConcurrentHashMap<>();

    public void check(String provider, Duration minimumInterval) {
        AtomicLong lastRequest = lastRequests.computeIfAbsent(provider,
                ignored -> new AtomicLong(Long.MIN_VALUE));

        while (true) {
            long now = System.nanoTime();
            long previous = lastRequest.get();
            if (previous != Long.MIN_VALUE && now - previous < minimumInterval.toNanos()) {
                throw new TrackProviderException(provider, "Search rate limit reached; use another provider or retry later");
            }
            if (lastRequest.compareAndSet(previous, now)) {
                return;
            }
        }
    }
}
