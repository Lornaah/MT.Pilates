package fr.mtpilates.bilan;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;

/**
 * Graphiques en aires du bilan (évolution du poids et des mensurations), dessinés en PNG comme ceux du PDF de
 * référence : grille pointillée, graduations à droite, semaines en abscisse, ligne d'objectif pour le poids.
 */
public final class BilanCharts {

    /** Rendu deux fois plus grand que l'affichage, pour la netteté à l'impression. */
    private static final int SCALE = 2;
    private static final Color INK = new Color(0x4A, 0x3F, 0x35);
    private static final Color TEXT = new Color(0x6E, 0x63, 0x58);
    private static final Color GRID = new Color(0xDD, 0xD6, 0xCE);
    private static final Color FILL = new Color(0xC6, 0xBD, 0xB3);
    private static final int LEFT = 30;
    private static final int RIGHT = 60;
    private static final int TOP = 36;
    private static final int BOTTOM = 56;
    private static final int TICKS = 3;

    /** Demi-hauteur de la fenêtre du poids autour de la pesée de départ, en kg. */
    private static final double WEIGHT_WINDOW = 10;

    public record Style(boolean aroundStart, boolean gradient) {
        public static final Style WEIGHT = new Style(true, true);
        public static final Style MEASURE = new Style(false, false);
    }

    private BilanCharts() {
    }

    /** Graphique en data URI PNG ; `width` et `height` en pixels CSS. */
    public static String area(final List<BilanCalculator.WeekPoint> points, final String yTitle, final Style style,
                              final Optional<Double> goal, final int width, final int height) {
        final BufferedImage image = new BufferedImage(width * SCALE, height * SCALE, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = image.createGraphics();
        g.scale(SCALE, SCALE);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        final double[] range = range(points, style, goal);
        final Plot plot = new Plot(LEFT, TOP, width - LEFT - RIGHT, height - TOP - BOTTOM, range[0], range[1], points.size());
        grid(g, plot, range[2], yTitle, width);
        labels(g, plot, points);
        series(g, plot, points, style);
        goal.ifPresent(v -> goalLine(g, plot, v));
        g.dispose();
        return BilanImages.pngDataUri(image);
    }

    private record Plot(int x, int y, int w, int h, double lo, double hi, int count) {
        double px(final int i) {
            return x + (i + 0.5) * w / count;
        }

        double py(final double value) {
            return y + h - (value - lo) / (hi - lo) * h;
        }
    }

    /**
     * Bornes et pas des graduations. Poids : départ ± 10 kg (élargi si une valeur ou l'objectif en sort), pour que la
     * courbe reste lisible même avec une petite perte. Mensuration : resserré autour des valeurs.
     */
    static double[] range(final List<BilanCalculator.WeekPoint> points, final Style style, final Optional<Double> goal) {
        final double max = Math.max(points.stream().mapToDouble(BilanCalculator.WeekPoint::value).max().orElse(1),
                goal.orElse(Double.NEGATIVE_INFINITY));
        final double min = Math.min(points.stream().mapToDouble(BilanCalculator.WeekPoint::value).min().orElse(0),
                goal.orElse(Double.POSITIVE_INFINITY));
        if (style.aroundStart() && !points.isEmpty()) {
            final double start = points.get(0).value();
            final double lo = Math.min(start - WEIGHT_WINDOW, min);
            final double hi = Math.max(start + WEIGHT_WINDOW, max);
            final double step = niceStep((hi - lo) / (TICKS * 2));
            return new double[] {Math.floor(lo / step) * step, Math.ceil(hi / step) * step, step};
        }
        final double step = niceStep(Math.max(1, max - min) / TICKS);
        return new double[] {Math.floor(min / step) * step - step, Math.ceil(max / step) * step, step};
    }

    static double niceStep(final double rough) {
        final double magnitude = Math.pow(10, Math.floor(Math.log10(rough)));
        final double ratio = rough / magnitude;
        final double nice = ratio <= 1 ? 1 : ratio <= 2 ? 2 : ratio <= 2.5 ? 2.5 : ratio <= 5 ? 5 : 10;
        return nice * magnitude;
    }

    private static void grid(final Graphics2D g, final Plot p, final double step, final String yTitle, final int width) {
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {4f, 4f}, 0f));
        g.setColor(GRID);
        g.draw(new RoundRectangle2D.Double(p.x(), p.y(), p.w(), p.h(), 10, 10));
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        final FontMetrics fm = g.getFontMetrics();
        for (double v = Math.ceil(p.lo() / step) * step; v <= p.hi() + 1e-9; v += step) {
            final int y = (int) Math.round(p.py(v));
            g.setColor(GRID);
            g.drawLine(p.x(), y, p.x() + p.w(), y);
            g.setColor(TEXT);
            g.drawString(BilanVariables.number(v), p.x() + p.w() + 8, y + fm.getAscent() / 2 - 1);
        }
        for (int i = 0; i < p.count(); i++) {
            final int x = (int) Math.round(p.px(i));
            g.setColor(GRID);
            g.drawLine(x, p.y(), x, p.y() + p.h());
        }
        g.setColor(TEXT);
        g.drawString(yTitle, width - fm.stringWidth(yTitle) - 6, p.y() - 10);
    }

    private static void labels(final Graphics2D g, final Plot p, final List<BilanCalculator.WeekPoint> points) {
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        final FontMetrics fm = g.getFontMetrics();
        g.setColor(TEXT);
        for (int i = 0; i < points.size(); i++) {
            final String label = points.get(i).label();
            g.drawString(label, (int) Math.round(p.px(i) - fm.stringWidth(label) / 2.0), p.y() + p.h() + 20);
        }
        final String axis = "Semaine";
        g.drawString(axis, p.x() + (p.w() - fm.stringWidth(axis)) / 2, p.y() + p.h() + 38);
    }

    private static void series(final Graphics2D g, final Plot p, final List<BilanCalculator.WeekPoint> points,
                               final Style style) {
        final Path2D line = new Path2D.Double();
        for (int i = 0; i < points.size(); i++) {
            final double x = p.px(i);
            final double y = p.py(points.get(i).value());
            if (i == 0) {
                line.moveTo(x, y);
            } else {
                line.lineTo(x, y);
            }
        }
        final Path2D area = new Path2D.Double(line);
        final double bottom = p.y() + p.h();
        area.lineTo(p.px(points.size() - 1), bottom);
        area.lineTo(p.px(0), bottom);
        area.closePath();
        if (style.gradient()) {
            final double top = points.stream().mapToDouble(pt -> p.py(pt.value())).min().orElse(p.y());
            g.setPaint(new GradientPaint(0, (float) top, new Color(0x4A, 0x3B, 0x2E, 235), 0,
                    (float) (top + (bottom - top) * 0.75), new Color(0x4A, 0x3B, 0x2E, 0)));
        } else {
            g.setPaint(FILL);
        }
        g.fill(area);
        g.setColor(INK);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(line);
    }

    private static void goalLine(final Graphics2D g, final Plot p, final double goal) {
        final int y = (int) Math.round(p.py(goal));
        g.setColor(INK);
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {3f, 3f}, 0f));
        g.drawLine(p.x(), y, p.x() + p.w(), y);
        final String label = "Objectif : " + BilanVariables.number(goal);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        final FontMetrics fm = g.getFontMetrics();
        final int w = fm.stringWidth(label) + 14;
        g.setColor(new Color(0x2A, 0x25, 0x20));
        g.fill(new RoundRectangle2D.Double(p.x() - 4, y - 10, w, 20, 8, 8));
        g.setColor(Color.WHITE);
        g.drawString(label, p.x() + 3, y + fm.getAscent() / 2 - 1);
    }
}
