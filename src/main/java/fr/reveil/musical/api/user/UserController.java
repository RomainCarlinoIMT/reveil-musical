package fr.reveil.musical.api.user;

import fr.reveil.musical.application.service.UserAccountService;
import fr.reveil.musical.domain.UserAccount;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserAccountService userAccountService;

    public UserController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody CreateUserRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body must not be null");
        }
        UserAccount account = userAccountService.register(request.pseudonym());
        UserResponse response = new UserResponse(account.id().value(), account.pseudonym());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
