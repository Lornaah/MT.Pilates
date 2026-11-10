package fr.mtpilates.shared.ui;

import java.util.Locale;

/** Tons de la carte statistique (StatCard de l'original) et leurs classes CSS. */
public enum StatTone {
    DEFAULT("", "bg-muted text-muted-foreground", "text-muted-foreground", "", "text-muted-foreground"),
    PRIMARY("", "bg-primary/10 text-primary", "text-muted-foreground", "", "text-muted-foreground"),
    ACCENT("bg-accent/20", "bg-primary/10 text-primary", "text-muted-foreground", "text-primary", "text-muted-foreground"),
    SAGE("", "bg-sage/15 text-sage", "text-muted-foreground", "", "text-muted-foreground"),
    DARK("bg-foreground text-background", "bg-background/10 text-background", "text-background/70", "", "text-background/60"),
    SECONDARY("", "bg-secondary/15 text-secondary", "text-muted-foreground", "", "text-muted-foreground");

    private final String card;
    private final String icon;
    private final String label;
    private final String value;
    private final String hint;

    StatTone(final String card, final String icon, final String label, final String value, final String hint) {
        this.card = card;
        this.icon = icon;
        this.label = label;
        this.value = value;
        this.hint = hint;
    }

    /** Ton à partir de son nom dans les templates (ex. « sage ») ; DEFAULT si absent. */
    public static StatTone of(final String name) {
        return name == null || name.isBlank() ? DEFAULT : valueOf(name.toUpperCase(Locale.ROOT));
    }

    public String card() {
        return card;
    }

    public String icon() {
        return icon;
    }

    public String label() {
        return label;
    }

    public String value() {
        return value;
    }

    public String hint() {
        return hint;
    }
}
