package fr.reveil.musical;

import fr.reveil.musical.application.port.TrackProvider;
import fr.reveil.musical.infrastructure.track.ResilientTrackProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@SpringBootTest
class ReveilMusicalApplicationTests {

    @Autowired
    private TrackProvider trackProvider;

    @Test
    void contextLoads() {
        assertInstanceOf(ResilientTrackProvider.class, trackProvider);
    }
}
