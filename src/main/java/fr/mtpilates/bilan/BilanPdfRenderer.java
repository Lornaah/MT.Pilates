package fr.mtpilates.bilan;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/** Bilan PDF : gabarit XHTML (Thymeleaf) converti par openhtmltopdf, polices standard du PDF. */
@Component
public class BilanPdfRenderer {

    /** Pictogrammes en data URI, accessibles au gabarit sous `icons`. */
    public record Icons(String note, String check, String arrow, String circleCheck) {
    }

    private static final Icons ICONS = new Icons(BilanIcons.NOTE_ICON, BilanIcons.CHECK_ICON, BilanIcons.ARROW_ICON,
            BilanIcons.CIRCLE_CHECK_ICON);

    private final SpringTemplateEngine templates;

    public BilanPdfRenderer(final SpringTemplateEngine templates) {
        this.templates = templates;
    }

    public String html(final BilanDocument document, final String title) {
        final Context context = new Context(java.util.Locale.FRENCH);
        context.setVariable("doc", document);
        context.setVariable("icons", ICONS);
        context.setVariable("title", title);
        return templates.process("pdf/bilan", context);
    }

    public byte[] pdf(final BilanDocument document, final String title) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            final PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html(document, title), null);
            builder.toStream(out);
            builder.run();
        } catch (final IOException e) {
            throw new UncheckedIOException("Génération du bilan PDF impossible", e);
        }
        return out.toByteArray();
    }
}
