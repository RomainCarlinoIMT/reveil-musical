package fr.reveil.musical.application.port;

import fr.reveil.musical.domain.UserAccount;
import fr.reveil.musical.domain.UserId;

import java.util.Optional;

public interface UserAccountRepository {

    void save(UserAccount userAccount);

    Optional<UserAccount> findById(UserId userId);
}
