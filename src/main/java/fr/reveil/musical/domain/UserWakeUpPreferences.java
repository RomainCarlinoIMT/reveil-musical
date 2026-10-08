package fr.reveil.musical.domain;

import java.time.LocalTime;
import java.util.Objects;

public record UserWakeUpPreferences(
        NotificationChannel notificationChannel,
        LocalTime sendTime) {

    public UserWakeUpPreferences {
        Objects.requireNonNull(notificationChannel, "notificationChannel must not be null");
        Objects.requireNonNull(sendTime, "sendTime must not be null");
    }
}
