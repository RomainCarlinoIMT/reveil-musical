package fr.reveil.musical.api.user;

import fr.reveil.musical.application.service.UserPreferencesService;
import fr.reveil.musical.domain.NotificationChannel;
import fr.reveil.musical.domain.UserId;
import fr.reveil.musical.domain.UserWakeUpPreferences;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userId}/preferences/channel")
public class NotificationChannelController {

    private final UserPreferencesService userPreferencesService;

    public NotificationChannelController(UserPreferencesService userPreferencesService) {
        this.userPreferencesService = userPreferencesService;
    }

    @GetMapping
    public NotificationChannelResponse getChannel(@PathVariable String userId) {
        UserWakeUpPreferences preferences = userPreferencesService.getOrDefault(new UserId(userId));
        return new NotificationChannelResponse(preferences.notificationChannel());
    }

    @PutMapping
    public NotificationChannelResponse updateChannel(
            @PathVariable String userId,
            @RequestBody UpdateNotificationChannelRequest request) {
        if (request == null || request.channel() == null) {
            throw new IllegalArgumentException("channel must be provided");
        }
        UserWakeUpPreferences preferences = userPreferencesService.updateNotificationChannel(
                new UserId(userId), request.channel());
        return new NotificationChannelResponse(preferences.notificationChannel());
    }

    public record UpdateNotificationChannelRequest(NotificationChannel channel) {
    }

    public record NotificationChannelResponse(NotificationChannel channel) {
    }
}
