package fr.reveil.musical.infrastructure.notification.push;

public interface PushSender {

    void publishNotification(String deviceToken, String title, String content);
}
