package fr.reveil.musical.application.port;

import fr.reveil.musical.domain.UserId;
import fr.reveil.musical.domain.UserWakeUpPreferences;

import java.util.Optional;

public interface UserPreferencesProvider {

    void savePreferences(UserId userId, UserWakeUpPreferences preferences);

    Optional<UserWakeUpPreferences> findPreferences(UserId userId);
}
