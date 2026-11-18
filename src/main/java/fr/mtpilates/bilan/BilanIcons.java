package fr.mtpilates.bilan;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/**
 * Pictogrammes du bilan (coche, flèche, note) dessinés en PNG : les polices standard du PDF n'ont ni ✔ ni →.
 */
public final class BilanIcons {

    private static final int SIZE = 64;
    private static final Color INK = new Color(0x4A, 0x3F, 0x35);
    private static final Color CHECK = new Color(0x5B, 0x6B, 0x7A);

    public static final String CHECK_ICON = icon(g -> {
        g.setColor(CHECK);
        g.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        final Path2D p = new Path2D.Double();
        p.moveTo(12, 34);
        p.lineTo(26, 48);
        p.lineTo(54, 14);
        g.draw(p);
    });

    public static final String ARROW_ICON = icon(g -> {
        g.setColor(INK);
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(8, 32, 54, 32);
        final Path2D p = new Path2D.Double();
        p.moveTo(38, 16);
        p.lineTo(54, 32);
        p.lineTo(38, 48);
        g.draw(p);
    });

    public static final String NOTE_ICON = icon(g -> {
        g.setColor(INK);
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        final Path2D p = new Path2D.Double();
        p.moveTo(14, 10);
        p.lineTo(50, 10);
        p.lineTo(50, 38);
        p.lineTo(36, 54);
        p.lineTo(14, 54);
        p.closePath();
        g.draw(p);
        g.drawLine(36, 54, 36, 40);
        g.drawLine(36, 40, 50, 38);
    });

    public static final String CIRCLE_CHECK_ICON = icon(g -> {
        g.setColor(INK);
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawOval(6, 6, 52, 52);
        final Path2D p = new Path2D.Double();
        p.moveTo(20, 33);
        p.lineTo(29, 42);
        p.lineTo(45, 23);
        g.draw(p);
    });

    private BilanIcons() {
    }

    private static String icon(final Consumer<Graphics2D> draw) {
        final BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        draw.accept(g);
        g.dispose();
        return BilanImages.pngDataUri(image);
    }
}
