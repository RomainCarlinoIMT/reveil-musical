package fr.reveil.musical.infrastructure.notification.push;

import fr.reveil.musical.application.port.NotificationAdapter;
import fr.reveil.musical.domain.NotificationChannel;
import fr.reveil.musical.domain.Track;
import fr.reveil.musical.domain.UserId;
import org.springframework.stereotype.Component;

@Component
public class PushNotificationAdapter implements NotificationAdapter {

    private final PushSender pushSender;

    public PushNotificationAdapter(PushSender pushSender) {
        this.pushSender = pushSender;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.PUSH;
    }

    @Override
    public void send(UserId userId, Track track) {
        String content = "Votre morceau du jour : " + track.title() + " - " + track.artist();
        pushSender.publishNotification(userId.value(), "Votre réveil musical", content);
    }
}
