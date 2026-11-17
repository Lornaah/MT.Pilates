package fr.mtpilates.shared.ui;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Component;

/** Rendu markdown → HTML. Le HTML brut est échappé et les liens assainis, comme ReactMarkdown par défaut. */
@Component
public class Markdown {

    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder().escapeHtml(true).sanitizeUrls(true).build();

    public String toHtml(final String markdown) {
        return renderer.render(parser.parse(markdown == null ? "" : markdown));
    }
}
