package fr.reveil.musical.infrastructure.notification;

import fr.reveil.musical.domain.NotificationChannel;
import fr.reveil.musical.domain.Track;
import fr.reveil.musical.domain.UserId;
import fr.reveil.musical.infrastructure.notification.email.EmailNotificationAdapter;
import fr.reveil.musical.infrastructure.notification.push.PushNotificationAdapter;
import fr.reveil.musical.infrastructure.notification.sms.SmsNotificationAdapter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationAdaptersTest {

    private static final UserId USER_ID = new UserId("user-123");
    private static final Track TRACK = new Track("Matin calme", "Artiste");

    @Test
    void emailAdapterMapsTrackToEmailSpecificArguments() {
        String[] sent = new String[3];
        EmailNotificationAdapter adapter = new EmailNotificationAdapter(
                (recipient, subject, body) -> {
                    sent[0] = recipient;
                    sent[1] = subject;
                    sent[2] = body;
                });

        assertEquals(NotificationChannel.EMAIL, adapter.channel());
        adapter.send(USER_ID, TRACK);

        assertEquals("user-123", sent[0]);
        assertEquals("Votre réveil musical", sent[1]);
        assertEquals("Votre morceau du jour : Matin calme - Artiste", sent[2]);
    }

    @Test
    void smsAdapterMapsTrackToSmsSpecificArguments() {
        String[] sent = new String[2];
        SmsNotificationAdapter adapter = new SmsNotificationAdapter(
                (recipient, text) -> {
                    sent[0] = recipient;
                    sent[1] = text;
                });

        assertEquals(NotificationChannel.SMS, adapter.channel());
        adapter.send(USER_ID, TRACK);

        assertEquals("user-123", sent[0]);
        assertEquals("Réveil musical : Matin calme - Artiste", sent[1]);
    }

    @Test
    void pushAdapterMapsTrackToPushSpecificArguments() {
        String[] sent = new String[3];
        PushNotificationAdapter adapter = new PushNotificationAdapter(
                (recipient, title, content) -> {
                    sent[0] = recipient;
                    sent[1] = title;
                    sent[2] = content;
                });

        assertEquals(NotificationChannel.PUSH, adapter.channel());
        adapter.send(USER_ID, TRACK);

        assertEquals("user-123", sent[0]);
        assertEquals("Votre réveil musical", sent[1]);
        assertEquals("Votre morceau du jour : Matin calme - Artiste", sent[2]);
    }
}
