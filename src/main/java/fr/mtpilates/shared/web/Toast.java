package fr.mtpilates.shared.web;

import java.util.Objects;

/** Notification éphémère affichée en bas à droite (équivalent des toasts « sonner » de l'original). */
public record Toast(String message, Category category) {

    public enum Category { SUCCESS, ERROR, INFO }

    public Toast {
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(category, "category");
    }

    public static Toast success(final String message) {
        return new Toast(message, Category.SUCCESS);
    }

    public static Toast error(final String message) {
        return new Toast(message, Category.ERROR);
    }

    public static Toast info(final String message) {
        return new Toast(message, Category.INFO);
    }
}
