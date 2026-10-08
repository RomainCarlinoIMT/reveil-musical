package fr.reveil.musical.application.service;

import fr.reveil.musical.application.port.UserAccountRepository;
import fr.reveil.musical.application.port.UserPreferencesProvider;
import fr.reveil.musical.domain.NotificationChannel;
import fr.reveil.musical.domain.UserId;
import fr.reveil.musical.domain.UserWakeUpPreferences;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.NoSuchElementException;
import java.util.Objects;

@Service
public class UserPreferencesService {

    public static final UserWakeUpPreferences DEFAULT_PREFERENCES =
            new UserWakeUpPreferences(NotificationChannel.EMAIL, LocalTime.of(7, 0));

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
        if (userAccountRepository.findById(userId).isEmpty()) {
            throw new NoSuchElementException("No user account found for ID " + userId.value());
        }
        return userPreferencesProvider.findPreferences(userId)
                .orElseThrow(() -> new NoSuchElementException(
                        "No wake-up preferences found for user ID " + userId.value()));
    }

    public UserWakeUpPreferences getOrDefault(UserId userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        if (userAccountRepository.findById(userId).isEmpty()) {
            throw new NoSuchElementException("No user account found for ID " + userId.value());
        }
        return userPreferencesProvider.findPreferences(userId).orElse(DEFAULT_PREFERENCES);
    }

    public UserWakeUpPreferences updateNotificationChannel(
            UserId userId,
            NotificationChannel notificationChannel) {
        Objects.requireNonNull(notificationChannel, "notificationChannel must not be null");
        UserWakeUpPreferences currentPreferences = getOrDefault(userId);
        UserWakeUpPreferences updatedPreferences =
                new UserWakeUpPreferences(notificationChannel, currentPreferences.sendTime());
        userPreferencesProvider.savePreferences(userId, updatedPreferences);
        return updatedPreferences;
    }
}
