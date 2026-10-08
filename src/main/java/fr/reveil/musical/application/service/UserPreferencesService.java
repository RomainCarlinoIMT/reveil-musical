package fr.reveil.musical.application.service;

import fr.reveil.musical.application.port.UserAccountRepository;
import fr.reveil.musical.application.port.UserPreferencesProvider;
import fr.reveil.musical.domain.UserId;
import fr.reveil.musical.domain.UserWakeUpPreferences;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.Objects;

@Service
public class UserPreferencesService {

    private final UserAccountRepository userAccountRepository;
    private final UserPreferencesProvider userPreferencesProvider;

    public UserPreferencesService(
            UserAccountRepository userAccountRepository,
            UserPreferencesProvider userPreferencesProvider) {
        this.userAccountRepository = userAccountRepository;
        this.userPreferencesProvider = userPreferencesProvider;
    }

    public void save(UserId userId, UserWakeUpPreferences preferences) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(preferences, "preferences must not be null");
        if (userAccountRepository.findById(userId).isEmpty()) {
            throw new NoSuchElementException("No user account found for ID " + userId.value());
        }
        userPreferencesProvider.savePreferences(userId, preferences);
    }

    public UserWakeUpPreferences get(UserId userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        return userPreferencesProvider.findPreferences(userId)
                .orElseThrow(() -> new NoSuchElementException(
                        "No wake-up preferences found for user ID " + userId.value()));
    }
}
