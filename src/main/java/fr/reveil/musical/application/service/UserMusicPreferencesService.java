package fr.reveil.musical.application.service;

import fr.reveil.musical.application.port.UserAccountRepository;
import fr.reveil.musical.application.port.UserMusicPreferencesProvider;
import fr.reveil.musical.domain.UserId;
import fr.reveil.musical.domain.UserMusicPreferences;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class UserMusicPreferencesService {

    private final UserAccountRepository userAccountRepository;
    private final UserMusicPreferencesProvider musicPreferencesProvider;

    public UserMusicPreferencesService(
            UserAccountRepository userAccountRepository,
            UserMusicPreferencesProvider musicPreferencesProvider) {
        this.userAccountRepository = userAccountRepository;
        this.musicPreferencesProvider = musicPreferencesProvider;
    }

    public UserMusicPreferences save(UserId userId, UserMusicPreferences preferences) {
        if (userId == null || preferences == null) {
            throw new IllegalArgumentException("userId and preferences must be provided");
        }
        if (userAccountRepository.findById(userId).isEmpty()) {
            throw new NoSuchElementException("No user account found for ID " + userId.value());
        }

        musicPreferencesProvider.save(userId, preferences);
        return preferences;
    }

    public Optional<UserMusicPreferences> findByUserId(UserId userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        if (userAccountRepository.findById(userId).isEmpty()) {
            throw new NoSuchElementException("No user account found for ID " + userId.value());
        }
        return musicPreferencesProvider.findByUserId(userId);
    }
}
