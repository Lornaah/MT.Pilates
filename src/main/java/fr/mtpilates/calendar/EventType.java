package fr.mtpilates.calendar;

import java.util.Arrays;
import java.util.Optional;

/** Types d'événement (eventTypeConfig.ts) : libellé, variante de badge, pastille du calendrier. */
public enum EventType {
    LIVE_CLASS("live_class", "Cours en ligne", "success", "bg-success"),
    STUDIO_CLASS("studio_class", "Cours studio", "sage", "bg-sage"),
    ONE_ON_ONE("one_on_one", "Suivi individuel", "default", "bg-primary"),
    CALL("call", "Appel", "secondary", "bg-secondary"),
    ADMIN_TASK("admin_task", "Premier RDV", "warm", "bg-trial");

    private final String code;
    private final String label;
    private final String variant;
    private final String dot;

    EventType(final String code, final String label, final String variant, final String dot) {
        this.code = code;
        this.label = label;
        this.variant = variant;
        this.dot = dot;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public String variant() {
        return variant;
    }

    public String dot() {
        return dot;
    }

    public static Optional<EventType> fromCode(final String code) {
        return Arrays.stream(values()).filter(t -> t.code.equals(code)).findFirst();
    }
}
