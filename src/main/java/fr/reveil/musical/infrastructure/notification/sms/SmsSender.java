package fr.reveil.musical.infrastructure.notification.sms;

public interface SmsSender {

    void sendSms(String phoneNumber, String text);
}
