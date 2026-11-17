package fr.mtpilates.shared.ui;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Liens `wa.me` de l'original (waLink) : numéro réduit à ses chiffres, message encodé comme encodeURIComponent. */
public final class WhatsAppLinks {

    private WhatsAppLinks() {
    }

    public static String link(final String phone, final String message) {
        final String digits = phone.replaceAll("[^\\d+]", "").replaceFirst("^\\+", "");
        return "https://wa.me/" + digits + "?text=" + encodeUriComponent(message);
    }

    /** URLEncoder suit les formulaires HTML : on rétablit les caractères qu'encodeURIComponent laisse tels quels. */
    static String encodeUriComponent(final String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20").replace("%21", "!").replace("%27", "'")
                .replace("%28", "(").replace("%29", ")").replace("%7E", "~");
    }
}
