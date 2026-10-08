package fr.reveil.musical.infrastructure.user;

import fr.reveil.musical.application.port.UserMusicPreferencesProvider;
import fr.reveil.musical.domain.UserId;
import fr.reveil.musical.domain.UserMusicPreferences;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class InMemoryUserMusicPreferencesProvider implements UserMusicPreferencesProvider {

    private final ConcurrentMap<UserId, UserMusicPreferences> preferences = new ConcurrentHashMap<>();

    @Override
    public void save(UserId userId, UserMusicPreferences userPreferences) {
        preferences.put(userId, userPreferences);
    }

    @Override
    public Optional<UserMusicPreferences> findByUserId(UserId userId) {
        return Optional.ofNullable(preferences.get(userId));
    }
}
