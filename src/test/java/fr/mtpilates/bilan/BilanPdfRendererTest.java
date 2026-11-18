package fr.mtpilates.bilan;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/** Rendu complet du bilan sur des données fictives : nombre de pages, textes et chiffres attendus. */
class BilanPdfRendererTest {

    private static final LocalDate START = LocalDate.of(2026, 10, 1);

    @Test
    void rendersEveryPageOfTheReferenceLayout() throws IOException {
        final BilanCalculator.Snapshot snapshot = BilanCalculator.compute(entries(), START, START.plusDays(28),
                Optional.empty(), Optional.of(55.0));
        final Map<String, String> vars = BilanVariables.of("Rina", snapshot,
                BilanVariables.computedDurations(snapshot, Optional.of(START)));
        final BilanContent content = new BilanContent(Map.of(), List.of("weight_kg", "waist_cm", "hip_cm", "thigh_cm", "arm_cm"),
                Map.of("back", view(), "side", view()), BilanTemplateCatalog.defaults());
        final byte[] photo = BilanImages.jpeg(photo());
        final BilanDocument doc = BilanDocumentFactory.build(content, snapshot, vars, id -> Optional.of(photo));

        final byte[] pdf = renderer().pdf(doc, "Bilan de Rina");

        try (PDDocument document = Loader.loadPDF(pdf)) {
            final Path dir = Path.of("target", "bilan-sample");
            Files.createDirectories(dir);
            Files.write(dir.resolve("bilan.pdf"), pdf);
            final PDFRenderer pages = new PDFRenderer(document);
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                javax.imageio.ImageIO.write(pages.renderImageWithDPI(i, 80), "png", dir.resolve("page-" + (i + 1) + ".png").toFile());
            }
            // couverture, chiffres, 2 vues, poids, 4 mensurations, sens, important, objectif, fierté
            assertThat(document.getNumberOfPages()).isEqualTo(13);
            final String text = new PDFTextStripper().getText(document).replaceAll("\\s+", " ").replace('\u2019', '\'');
            assertThat(text).contains("TON 1er MOIS DE TRANSFORMATION", "-3,3 kg", "Poids perdu", "TA TRANSFORMATION VUE DE DOS",
                    "L'ÉVOLUTION DE TES HANCHES", "27,5%", "Progrès réalisé", "SOIS FIÈRE DE TOI");
        }
    }

    static BilanPdfRenderer renderer() {
        final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        final SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return new BilanPdfRenderer(engine);
    }

    private static BilanContent.View view() {
        return new BilanContent.View(true, "before", "after",
                List.of(new BilanContent.Stroke("#e11d48", 0.01, List.of(List.of(0.2, 0.3), List.of(0.4, 0.5), List.of(0.3, 0.7)))),
                List.of(new BilanContent.Stroke("#16a34a", 0.01, List.of(List.of(0.2, 0.3), List.of(0.4, 0.5)))));
    }

    private static BufferedImage photo() {
        final BufferedImage image = new BufferedImage(600, 900, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = image.createGraphics();
        g.setPaint(new GradientPaint(0, 0, new Color(0xB0, 0x6A, 0x4B), 0, 900, new Color(0x6B, 0x4E, 0x3D)));
        g.fillRect(0, 0, 600, 900);
        g.dispose();
        return image;
    }

    private static List<BilanCalculator.Entry> entries() {
        return List.of(entry(0, "67", "80", "97", "58", "33"), entry(8, "66", "78", "96", "57.2", "32.5"),
                entry(15, "65.2", "77", "95", "56.5", "32"), entry(22, "64.5", "76", "94", "55.8", "31.5"),
                entry(28, "63.7", "75", "93", "55", "31"));
    }

    private static BilanCalculator.Entry entry(final int day, final String weight, final String waist, final String hip,
                                               final String thigh, final String arm) {
        final Map<BilanMetric, BigDecimal> values = new EnumMap<>(BilanMetric.class);
        values.put(BilanMetric.WEIGHT, new BigDecimal(weight));
        values.put(BilanMetric.WAIST, new BigDecimal(waist));
        values.put(BilanMetric.HIP, new BigDecimal(hip));
        values.put(BilanMetric.THIGH, new BigDecimal(thigh));
        values.put(BilanMetric.ARM, new BigDecimal(arm));
        return new BilanCalculator.Entry(START.plusDays(day), values);
    }
}
