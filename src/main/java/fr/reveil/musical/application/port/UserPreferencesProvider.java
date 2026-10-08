package fr.reveil.musical.application.port;

import fr.reveil.musical.domain.UserId;
import fr.reveil.musical.domain.UserWakeUpPreferences;

public interface UserPreferencesProvider {

    UserWakeUpPreferences findPreferences(UserId userId);
}
