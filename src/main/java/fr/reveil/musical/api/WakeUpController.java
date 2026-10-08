package fr.reveil.musical.api;

import fr.reveil.musical.application.port.WakeUpService;
import fr.reveil.musical.application.service.UserAccountService;
import fr.reveil.musical.domain.UserId;
import fr.reveil.musical.domain.WakeUpRequest;
import fr.reveil.musical.domain.WeatherType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;

@RestController
@RequestMapping("/api/users/{userId}/wake-up")
public class WakeUpController {

    private final UserAccountService userAccountService;
    private final WakeUpService wakeUpService;

    public WakeUpController(UserAccountService userAccountService, WakeUpService wakeUpService) {
        this.userAccountService = userAccountService;
        this.wakeUpService = wakeUpService;
    }

    @PostMapping
    public ResponseEntity<Void> wakeUp(
            @PathVariable String userId,
            @RequestParam DayOfWeek day,
            @RequestParam WeatherType weather) {
        UserId id = new UserId(userId);
        userAccountService.get(id);
        wakeUpService.wakeUp(new WakeUpRequest(id, day, weather));
        return ResponseEntity.noContent().build();
    }
}
