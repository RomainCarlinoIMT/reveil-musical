package fr.reveil.musical.application.port;

import fr.reveil.musical.domain.WakeUpRequest;

public interface WakeUpService {

    void wakeUp(WakeUpRequest request);
}
