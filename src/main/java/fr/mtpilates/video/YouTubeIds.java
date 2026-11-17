package fr.mtpilates.video;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** extractYouTubeId de l'original : identifiant de 11 caractères seul ou extrait d'une URL YouTube. */
public final class YouTubeIds {

    private static final Pattern BARE = Pattern.compile("^[\\w-]{11}$");
    private static final Pattern URL = Pattern.compile("(?:youtube\\.com/(?:watch\\?v=|embed/|v/)|youtu\\.be/)([\\w-]{11})");

    private YouTubeIds() {
    }

    public static Optional<String> extract(final String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }
        final String trimmed = input.trim();
        if (BARE.matcher(trimmed).matches()) {
            return Optional.of(trimmed);
        }
        final Matcher matcher = URL.matcher(trimmed);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }
}
