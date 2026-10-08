package fr.reveil.musical.application.service;

import fr.reveil.musical.domain.UserAccount;
import fr.reveil.musical.domain.UserId;
import fr.reveil.musical.infrastructure.user.InMemoryUserAccountRepository;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserAccountServiceTest {

    private final InMemoryUserAccountRepository repository = new InMemoryUserAccountRepository();
    private final UserAccountService service = new UserAccountService(repository);

    @Test
    void registersAccountWithTrimmedPseudonymAndUuid() {
        UserAccount account = service.register("  Camille  ");

        assertEquals("Camille", account.pseudonym());
        assertEquals(account, repository.findById(account.id()).orElseThrow());
        assertEquals(account.id().value(), UUID.fromString(account.id().value()).toString());
    }

    @Test
    void eachRegistrationReceivesAnUniqueUserId() {
        UserAccount first = service.register("Camille");
        UserAccount second = service.register("Camille");

        assertNotEquals(first.id(), second.id());
    }

    @Test
    void rejectsNullAndBlankPseudonyms() {
        assertThrows(IllegalArgumentException.class, () -> service.register(null));
        assertThrows(IllegalArgumentException.class, () -> service.register("  "));
    }

    @Test
    void concurrentRegistrationsReceiveDistinctIds() throws Exception {
        var ids = ConcurrentHashMap.<UserId>newKeySet();
        try (var executor = Executors.newFixedThreadPool(8)) {
            var registrations = IntStream.range(0, 100)
                    .<Callable<Boolean>>mapToObj(
                            index -> () -> ids.add(service.register("user-" + index).id()))
                    .map(executor::submit)
                    .toList();

            for (var registration : registrations) {
                assertTrue(registration.get());
            }
        }
        assertEquals(100, ids.size());
    }
}
