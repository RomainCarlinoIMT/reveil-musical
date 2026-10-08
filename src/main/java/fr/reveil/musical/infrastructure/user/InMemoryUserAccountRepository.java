package fr.reveil.musical.infrastructure.user;

import fr.reveil.musical.application.port.UserAccountRepository;
import fr.reveil.musical.domain.UserAccount;
import fr.reveil.musical.domain.UserId;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class InMemoryUserAccountRepository implements UserAccountRepository {

    private final ConcurrentMap<UserId, UserAccount> accounts = new ConcurrentHashMap<>();

    @Override
    public void save(UserAccount userAccount) {
        UserAccount existing = accounts.putIfAbsent(userAccount.id(), userAccount);
        if (existing != null) {
            throw new IllegalStateException("A user account already exists for ID " + userAccount.id().value());
        }
    }

    @Override
    public Optional<UserAccount> findById(UserId userId) {
        return Optional.ofNullable(accounts.get(userId));
    }
}
