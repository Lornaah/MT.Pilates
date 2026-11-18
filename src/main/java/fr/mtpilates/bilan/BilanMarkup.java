package fr.mtpilates.bilan;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.web.util.HtmlUtils;

/**
 * Textes du bilan : variables {nom} remplacées, puis mise en forme légère (**gras**, ==surligné==, __souligné__,
 * retours à la ligne) convertie en XHTML pour le PDF. Le texte saisi est échappé avant toute balise.
 */
public final class BilanMarkup {

    private static final Pattern VARIABLE = Pattern.compile("\\{([a-z_]+)}");
    private static final Pattern BOLD = Pattern.compile("\\*\\*(.+?)\\*\\*", Pattern.DOTALL);
    private static final Pattern HIGHLIGHT = Pattern.compile("==(.+?)==", Pattern.DOTALL);
    private static final Pattern UNDERLINE = Pattern.compile("__(.+?)__", Pattern.DOTALL);

    /** Point d'une liste « Titre : texte ». */
    public record Point(String title, String text) {
    }

    private BilanMarkup() {
    }

    /** Remplace les variables connues ; une variable inconnue reste telle quelle. */
    public static String resolve(final String text, final Map<String, String> variables) {
        final Matcher m = VARIABLE.matcher(text == null ? "" : text);
        final StringBuilder out = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(out, Matcher.quoteReplacement(variables.getOrDefault(m.group(1), m.group(0))));
        }
        m.appendTail(out);
        return out.toString();
    }

    /** Texte résolu, échappé puis mis en forme, prêt à insérer tel quel dans le XHTML du PDF. */
    public static String html(final String text, final Map<String, String> variables) {
        String html = HtmlUtils.htmlEscape(resolve(text, variables), "UTF-8");
        html = BOLD.matcher(html).replaceAll("<strong>$1</strong>");
        html = HIGHLIGHT.matcher(html).replaceAll("<span class=\"hl\">$1</span>");
        html = UNDERLINE.matcher(html).replaceAll("<u>$1</u>");
        return html.replace("\n", "<br/>");
    }

    /** Une ligne par point, titre et texte séparés par le premier « : ». */
    public static List<Point> points(final String text) {
        final List<Point> points = new ArrayList<>();
        for (final String line : (text == null ? "" : text).split("\n")) {
            final String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            final int colon = trimmed.indexOf(':');
            points.add(colon < 0 ? new Point(trimmed, "")
                    : new Point(trimmed.substring(0, colon).trim(), trimmed.substring(colon + 1).trim()));
        }
        return points;
    }
}
