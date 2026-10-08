package fr.reveil.musical.domain;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserMusicPreferencesTest {

    @Test
    void copiesTheConditionMapAndTrackListsAndNormalizesTrackTitles() {
        MusicCondition condition = new MusicCondition(DayOfWeek.MONDAY, WeatherType.SOLEIL);
        List<String> titles = new ArrayList<>(List.of(" Song one ", "Song two"));
        Map<MusicCondition, List<String>> tracks = new HashMap<>();
        tracks.put(condition, titles);

        UserMusicPreferences preferences = new UserMusicPreferences(tracks, " Default song ");

        titles.add("Song three");
        tracks.clear();
        assertEquals(List.of("Song one", "Song two"), preferences.tracksByCondition().get(condition));
        assertEquals("Default song", preferences.fallbackTrack());
        assertThrows(UnsupportedOperationException.class,
                () -> preferences.tracksByCondition().get(condition).add("Song four"));
    }

    @Test
    void rejectsEmptySongListForACondition() {
        MusicCondition condition = new MusicCondition(DayOfWeek.MONDAY, WeatherType.SOLEIL);

        assertThrows(IllegalArgumentException.class,
                () -> new UserMusicPreferences(Map.of(condition, List.of()), "Default song"));
    }
}
