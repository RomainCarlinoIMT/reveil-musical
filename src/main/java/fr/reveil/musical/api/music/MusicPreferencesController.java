package fr.reveil.musical.api.music;

import fr.reveil.musical.application.service.UserMusicPreferencesService;
import fr.reveil.musical.domain.UserId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userId}/music-preferences")
public class MusicPreferencesController {

    private final UserMusicPreferencesService musicPreferencesService;

    public MusicPreferencesController(UserMusicPreferencesService musicPreferencesService) {
        this.musicPreferencesService = musicPreferencesService;
    }

    @PostMapping
    public ResponseEntity<Void> saveMusicPreferences(
            @PathVariable String userId,
            @RequestBody SaveMusicPreferencesRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body must not be null");
        }
        musicPreferencesService.save(new UserId(userId), request.toDomain());
        return ResponseEntity.noContent().build();
    }
}
