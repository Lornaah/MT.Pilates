package fr.mtpilates.profile;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** URL de lecteur intégré pour la vidéo d'onboarding (toEmbedUrl de l'original). */
public final class VideoEmbed {

    private static final Pattern YOUTUBE = Pattern.compile("(?:youtube\\.com/watch\\?v=|youtu\\.be/)([\\w-]+)");
    private static final Pattern VIMEO = Pattern.compile("vimeo\\.com/(\\d+)");

    private VideoEmbed() {
    }

    public static Optional<String> of(final String url) {
        if (url == null || url.isEmpty()) {
            return Optional.empty();
        }
        final Matcher youtube = YOUTUBE.matcher(url);
        if (youtube.find()) {
            return Optional.of("https://www.youtube.com/embed/" + youtube.group(1));
        }
        final Matcher vimeo = VIMEO.matcher(url);
        if (vimeo.find()) {
            return Optional.of("https://player.vimeo.com/video/" + vimeo.group(1));
        }
        return Optional.of(url);
    }
}
