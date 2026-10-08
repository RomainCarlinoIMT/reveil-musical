package fr.reveil.musical.application.service;

import fr.reveil.musical.domain.MusicCondition;
import fr.reveil.musical.domain.UserId;
import fr.reveil.musical.domain.UserMusicPreferences;
import fr.reveil.musical.domain.WeatherType;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class MorningMusicService {

    private final UserMusicPreferencesService musicPreferencesService;

    public MorningMusicService(UserMusicPreferencesService musicPreferencesService) {
        this.musicPreferencesService = musicPreferencesService;
    }

    public List<String> findMusicFor(UserId userId, DayOfWeek day, WeatherType weather) {
        if (userId == null || day == null || weather == null) {
            throw new IllegalArgumentException("userId, day and weather must be provided");
        }

        UserMusicPreferences preferences = musicPreferencesService.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException(
                        "No music preferences found for user ID " + userId.value()));
        MusicCondition condition = new MusicCondition(day, weather);
        List<String> tracks = preferences.tracksByCondition().get(condition);
        return tracks == null ? List.of(preferences.fallbackTrack()) : tracks;
    }
}
