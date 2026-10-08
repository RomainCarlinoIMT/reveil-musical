package fr.reveil.musical.domain;

import java.util.Objects;

public record UserAccount(UserId id, String pseudonym) {

    public UserAccount {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(pseudonym, "pseudonym must not be null");
        pseudonym = pseudonym.strip();
        if (pseudonym.isBlank()) {
            throw new IllegalArgumentException("pseudonym must not be blank");
        }
    }
}
