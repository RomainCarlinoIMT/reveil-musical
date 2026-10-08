package fr.reveil.musical.application.port;

import fr.reveil.musical.domain.UserId;
import fr.reveil.musical.domain.UserMusicPreferences;

import java.util.Optional;

public interface UserMusicPreferencesProvider {

    void save(UserId userId, UserMusicPreferences preferences);

    Optional<UserMusicPreferences> findByUserId(UserId userId);
}
