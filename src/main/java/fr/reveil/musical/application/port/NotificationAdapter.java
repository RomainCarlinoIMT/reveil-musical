package fr.reveil.musical.application.port;

import fr.reveil.musical.domain.NotificationChannel;
import fr.reveil.musical.domain.Track;
import fr.reveil.musical.domain.UserId;

public interface NotificationAdapter {

    NotificationChannel channel();

    void send(UserId userId, Track track);
}
