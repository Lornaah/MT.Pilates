package fr.mtpilates.bilan;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/**
 * Photos du bilan : orientation EXIF appliquée (comme le navigateur qui a servi aux annotations), taille réduite,
 * traits du coach dessinés par-dessus, encodage JPEG en data URI pour le PDF.
 */
public final class BilanImages {

    private static final int MAX_SIDE = 1400;
    private static final float JPEG_QUALITY = 0.85f;
    private static final double MIN_STROKE = 0.002;

    private BilanImages() {
    }

    public static Optional<String> annotatedPhoto(final byte[] original, final List<BilanContent.Stroke> strokes) {
        return read(original).map(image -> {
            final BufferedImage scaled = scale(orient(image, ExifOrientation.of(original)));
            draw(scaled, strokes);
            return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(jpeg(scaled));
        });
    }

    static Optional<BufferedImage> read(final byte[] bytes) {
        try {
            return Optional.ofNullable(ImageIO.read(new ByteArrayInputStream(bytes)));
        } catch (final IOException e) {
            return Optional.empty();
        }
    }

    /** Traits en coordonnées relatives (0 à 1), épaisseur relative à la largeur, extrémités arrondies. */
    static void draw(final BufferedImage image, final List<BilanContent.Stroke> strokes) {
        final Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        final int w = image.getWidth();
        final int h = image.getHeight();
        for (final BilanContent.Stroke stroke : strokes) {
            final List<List<Double>> points = stroke.points() == null ? List.of() : stroke.points();
            if (points.isEmpty()) {
                continue;
            }
            g.setColor(color(stroke.color()));
            g.setStroke(new BasicStroke((float) (Math.max(MIN_STROKE, stroke.width()) * w), BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
            final Path2D path = new Path2D.Double();
            path.moveTo(points.get(0).get(0) * w, points.get(0).get(1) * h);
            points.stream().skip(1).forEach(p -> path.lineTo(p.get(0) * w, p.get(1) * h));
            if (points.size() == 1) {
                path.lineTo(points.get(0).get(0) * w + 0.1, points.get(0).get(1) * h);
            }
            g.draw(path);
        }
        g.dispose();
    }

    static Color color(final String css) {
        try {
            return css != null && css.matches("#[0-9a-fA-F]{6}") ? Color.decode(css) : Color.RED;
        } catch (final NumberFormatException e) {
            return Color.RED;
        }
    }

    private static BufferedImage orient(final BufferedImage image, final int orientation) {
        final int w = image.getWidth();
        final int h = image.getHeight();
        final AffineTransform t = new AffineTransform();
        final boolean swap = orientation >= 5;
        switch (orientation) {
            case 2 -> { t.translate(w, 0); t.scale(-1, 1); }
            case 3 -> { t.translate(w, h); t.rotate(Math.PI); }
            case 4 -> { t.translate(0, h); t.scale(1, -1); }
            case 5 -> t.setTransform(0, 1, 1, 0, 0, 0);
            case 6 -> { t.translate(h, 0); t.rotate(Math.PI / 2); }
            case 7 -> t.setTransform(0, -1, -1, 0, h, w);
            case 8 -> { t.translate(0, w); t.rotate(-Math.PI / 2); }
            default -> {
                return rgb(image);
            }
        }
        final BufferedImage out = new BufferedImage(swap ? h : w, swap ? w : h, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = out.createGraphics();
        g.drawImage(image, t, null);
        g.dispose();
        return out;
    }

    private static BufferedImage scale(final BufferedImage image) {
        final double ratio = Math.min(1, MAX_SIDE / (double) Math.max(image.getWidth(), image.getHeight()));
        final int w = (int) Math.round(image.getWidth() * ratio);
        final int h = (int) Math.round(image.getHeight() * ratio);
        final BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(image, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    private static BufferedImage rgb(final BufferedImage image) {
        if (image.getType() == BufferedImage.TYPE_INT_RGB) {
            return image;
        }
        final BufferedImage out = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = out.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return out;
    }

    static byte[] jpeg(final BufferedImage image) {
        final ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(stream);
            final ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);
            writer.write(null, new IIOImage(image, null, null), param);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }

    static byte[] png(final BufferedImage image) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", out);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    static String pngDataUri(final BufferedImage image) {
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(png(image));
    }
}
