package fr.reveil.musical.infrastructure.notification.email;

import fr.reveil.musical.application.port.NotificationAdapter;
import fr.reveil.musical.domain.NotificationChannel;
import fr.reveil.musical.domain.Track;
import fr.reveil.musical.domain.UserId;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificationAdapter implements NotificationAdapter {

    private final EmailSender emailSender;

    public EmailNotificationAdapter(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void send(UserId userId, Track track) {
        String message = "Votre morceau du jour : " + track.title() + " - " + track.artist();
        emailSender.sendEmail(userId.value(), "Votre réveil musical", message);
    }
}
