package fr.mtpilates.shared.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

/**
 * Conversions des champs de formulaire, avec la tolérance de l'original : un champ
 * vide devient « absent », un nombre illisible aussi.
 */
public final class FormValues {

    private FormValues() {
    }

    public static Optional<String> text(final String value) {
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value);
    }

    public static Optional<BigDecimal> decimal(final String value) {
        try {
            return text(value).map(v -> new BigDecimal(v.trim().replace(',', '.')));
        } catch (final NumberFormatException e) {
            return Optional.empty();
        }
    }

    public static Optional<Integer> integer(final String value) {
        return decimal(value).map(BigDecimal::intValue);
    }

    public static Optional<LocalDate> date(final String value) {
        try {
            return text(value).map(v -> LocalDate.parse(v.trim()));
        } catch (final DateTimeParseException e) {
            return Optional.empty();
        }
    }

    public static List<String> list(final List<String> values) {
        return values == null ? List.of() : values.stream().filter(v -> v != null && !v.isBlank()).toList();
    }
}
