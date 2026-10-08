package fr.reveil.musical.application.service;

import fr.reveil.musical.application.port.UserAccountRepository;
import fr.reveil.musical.domain.UserAccount;
import fr.reveil.musical.domain.UserId;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.NoSuchElementException;

@Service
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;

    public UserAccountService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    public UserAccount register(String pseudonym) {
        if (pseudonym == null) {
            throw new IllegalArgumentException("pseudonym must not be null");
        }
        String normalizedPseudonym = pseudonym.strip();
        if (normalizedPseudonym.isBlank()) {
            throw new IllegalArgumentException("pseudonym must not be blank");
        }

        UserAccount account = new UserAccount(
                new UserId(UUID.randomUUID().toString()),
                normalizedPseudonym);
        userAccountRepository.save(account);
        return account;
    }

    public UserAccount get(UserId userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException(
                        "No user account found for ID " + userId.value()));
    }
}
