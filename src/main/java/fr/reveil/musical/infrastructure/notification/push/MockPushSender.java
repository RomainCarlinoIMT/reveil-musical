package fr.reveil.musical.infrastructure.notification.push;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MockPushSender implements PushSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockPushSender.class);

    @Override
    public void publishNotification(String deviceToken, String title, String content) {
        LOGGER.info("MOCK notification type=PUSH recipient={} title={} message={}",
                deviceToken, title, content);
    }
}
