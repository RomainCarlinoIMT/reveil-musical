package fr.reveil.musical.infrastructure.notification.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MockSmsSender implements SmsSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockSmsSender.class);

    @Override
    public void sendSms(String phoneNumber, String text) {
        LOGGER.info("MOCK notification type=SMS recipient={} message={}", phoneNumber, text);
    }
}
