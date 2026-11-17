package fr.mtpilates.document;

import java.util.LinkedHashMap;
import java.util.Map;

/** Catégories, visibilités et icônes de fichier (types/document.ts, lib/fileIcons.ts). */
public final class DocumentCatalog {

    public static final Map<String, String> CATEGORIES = ordered("guide", "Guide", "fiche_pratique", "Fiche pratique",
            "contrat", "Contrat", "recette", "Recette", "autre", "Autre");
    public static final Map<String, String> VISIBILITIES = ordered("staff_only", "Équipe uniquement",
            "all_members", "Tous les membres", "specific", "Membres spécifiques");

    private DocumentCatalog() {
    }

    /** iconForMime : icône Lucide selon le type de fichier. */
    public static String icon(final String mime) {
        if (mime == null) {
            return "file";
        }
        if (mime.startsWith("image/")) {
            return "file-image";
        }
        if (mime.startsWith("video/")) {
            return "file-video";
        }
        if (mime.startsWith("audio/")) {
            return "file-audio";
        }
        if ("application/pdf".equals(mime)) {
            return "file-text";
        }
        if (mime.contains("spreadsheet") || mime.contains("excel") || "text/csv".equals(mime)) {
            return "file-spreadsheet";
        }
        if (mime.contains("zip") || mime.contains("rar") || mime.contains("tar")) {
            return "file-archive";
        }
        if (mime.contains("javascript") || mime.contains("json") || mime.contains("xml")) {
            return "file-code";
        }
        return mime.startsWith("text/") ? "file-text" : "file";
    }

    public static String visibilityIcon(final String visibility) {
        return switch (visibility) {
            case "all_members" -> "globe";
            case "specific" -> "users";
            default -> "lock";
        };
    }

    /** formatFileSize : « — » si inconnu, sinon o / Ko / Mo à une décimale. */
    public static String size(final Integer bytes) {
        if (bytes == null) {
            return "—";
        }
        if (bytes < 1024) {
            return bytes + " o";
        }
        if (bytes < 1024 * 1024) {
            return String.format(java.util.Locale.ROOT, "%.1f Ko", bytes / 1024.0);
        }
        return String.format(java.util.Locale.ROOT, "%.1f Mo", bytes / (1024.0 * 1024.0));
    }

    private static Map<String, String> ordered(final String... pairs) {
        final Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return map;
    }
}
