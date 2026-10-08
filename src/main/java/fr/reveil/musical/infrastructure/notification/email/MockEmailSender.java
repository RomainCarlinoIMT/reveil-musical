package fr.reveil.musical.infrastructure.notification.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MockEmailSender implements EmailSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockEmailSender.class);

    @Override
    public void sendEmail(String recipient, String subject, String body) {
        LOGGER.info("MOCK notification type=EMAIL recipient={} subject={} message={}",
                recipient, subject, body);
    }
}
