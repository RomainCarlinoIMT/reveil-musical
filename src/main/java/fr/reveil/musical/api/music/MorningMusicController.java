package fr.reveil.musical.api.music;

import fr.reveil.musical.application.service.MorningMusicService;
import fr.reveil.musical.domain.UserId;
import fr.reveil.musical.domain.WeatherType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/morning-music")
public class MorningMusicController {

    private final MorningMusicService morningMusicService;

    public MorningMusicController(MorningMusicService morningMusicService) {
        this.morningMusicService = morningMusicService;
    }

    @GetMapping
    public List<String> findMusicForMorning(
            @PathVariable String userId,
            @RequestParam DayOfWeek day,
            @RequestParam WeatherType weather) {
        return morningMusicService.findMusicFor(new UserId(userId), day, weather);
    }
}
