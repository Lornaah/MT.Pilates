package fr.mtpilates.video;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/** Séance affichée dans la bibliothèque ou le gestionnaire. */
public record VideoCard(UUID id, String title, String description, String youtubeId, String thumbnailUrl,
                        String levelCode, String levelLabel, Optional<Integer> durationMinutes, List<String> focusAreas) {

    /** Texte recherché par le filtre : titre et focus, en minuscules. */
    public String searchText() {
        return (title + "\n" + String.join("\n", focusAreas)).toLowerCase(Locale.ROOT);
    }
}
