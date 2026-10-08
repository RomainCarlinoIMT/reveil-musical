package fr.reveil.musical.infrastructure.notification.email;

public interface EmailSender {

    void sendEmail(String recipient, String subject, String body);
}
