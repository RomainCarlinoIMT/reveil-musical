package fr.reveil.musical.infrastructure.notification.sms;

import fr.reveil.musical.application.port.NotificationAdapter;
import fr.reveil.musical.domain.NotificationChannel;
import fr.reveil.musical.domain.Track;
import fr.reveil.musical.domain.UserId;
import org.springframework.stereotype.Component;

@Component
public class SmsNotificationAdapter implements NotificationAdapter {

    private final SmsSender smsSender;

    public SmsNotificationAdapter(SmsSender smsSender) {
        this.smsSender = smsSender;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SMS;
    }

    @Override
    public void send(UserId userId, Track track) {
        String message = "Réveil musical : " + track.title() + " - " + track.artist();
        smsSender.sendSms(userId.value(), message);
    }
}
