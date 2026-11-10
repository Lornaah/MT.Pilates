package fr.mtpilates.shared.ui;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/**
 * Icônes Lucide rendues en SVG inline côté serveur, comme lucide-react dans
 * l'original. Utilisation Thymeleaf : {@code th:utext="${icons.svg('calendar', 'h-4 w-4')}"}.
 */
@Component
public class IconRegistry {

    private static final Pattern VALID_NAME = Pattern.compile("[a-z0-9-]+");
    private static final Pattern CLASS_ATTRIBUTE = Pattern.compile("class=\"[^\"]*\"");

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public String svg(final String name, final String cssClass) {
        final String template = cache.computeIfAbsent(name, IconRegistry::read);
        final String classes = "lucide lucide-" + name + " " + HtmlUtils.htmlEscape(cssClass);
        return CLASS_ATTRIBUTE.matcher(template)
                .replaceFirst("class=\"" + classes.trim() + "\" aria-hidden=\"true\"");
    }

    public String svg(final String name) {
        return svg(name, "");
    }

    private static String read(final String name) {
        if (!VALID_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Nom d'icône invalide : " + name);
        }
        final ClassPathResource resource = new ClassPathResource("icons/lucide/" + name + ".svg");
        try (InputStream in = resource.getInputStream()) {
            return stripComment(new String(in.readAllBytes(), StandardCharsets.UTF_8)).strip();
        } catch (final IOException e) {
            throw new IllegalArgumentException("Icône Lucide inconnue : " + name, e);
        }
    }

    private static String stripComment(final String svg) {
        // Les fichiers lucide-static commencent par un commentaire de licence, inutile dans le HTML.
        return svg.replaceFirst("(?s)^<!--.*?-->", "");
    }
}
