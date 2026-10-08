package fr.reveil.musical.infrastructure.user;

import fr.reveil.musical.application.port.UserPreferencesProvider;
import fr.reveil.musical.domain.UserId;
import fr.reveil.musical.domain.UserWakeUpPreferences;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class InMemoryUserPreferencesProvider implements UserPreferencesProvider {

    private final ConcurrentMap<UserId, UserWakeUpPreferences> preferences = new ConcurrentHashMap<>();

    @Override
    public void savePreferences(UserId userId, UserWakeUpPreferences userPreferences) {
        preferences.put(userId, userPreferences);
    }

    @Override
    public Optional<UserWakeUpPreferences> findPreferences(UserId userId) {
        return Optional.ofNullable(preferences.get(userId));
    }
}
