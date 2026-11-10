package fr.mtpilates.shared.storage;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Emplacement d'un fichier : un « bucket » (ex. member-photos) et un chemin
 * relatif (ex. {userId}/1700000000-front.jpg), comme dans l'original.
 */
public record StorageKey(String bucket, String path) {

    private static final Pattern SAFE_SEGMENT = Pattern.compile("[A-Za-z0-9._-]+");

    public StorageKey {
        Objects.requireNonNull(bucket, "bucket");
        Objects.requireNonNull(path, "path");
        requireSafe(bucket);
        for (final String segment : path.split("/", -1)) {
            requireSafe(segment);
        }
    }

    private static void requireSafe(final String segment) {
        // Interdit « .. », segments vides et caractères spéciaux : la clé finit en chemin disque.
        if (!SAFE_SEGMENT.matcher(segment).matches() || segment.equals(".") || segment.equals("..")) {
            throw new IllegalArgumentException("Segment de chemin invalide : " + segment);
        }
    }
}
