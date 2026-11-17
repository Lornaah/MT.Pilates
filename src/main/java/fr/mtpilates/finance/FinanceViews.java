package fr.mtpilates.finance;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Vues de la page Finance. */
public final class FinanceViews {

    private FinanceViews() {
    }

    /** Ligne du tableau ; `phone` est le numéro normalisé pour WhatsApp (sanitizePhone). */
    public record Row(UUID id, String name, String email, String status, String statusLabel, String statusClasses,
                      Optional<String> source, boolean sourceFromProspect, String contracted, String paid,
                      String remaining, boolean hasRemaining, boolean hasContract, double paidRatio,
                      Optional<String> rawPhone, String phone, String reminder) {
    }

    public record Page(List<Row> rows, int activeCount, String contracted, String paid, String remaining,
                       boolean hasRemaining, Map<String, Object> charts, List<String> sources) {
    }

    /** Valeurs du dialog « Suivi financier ». */
    public record EditValues(UUID id, String title, Optional<String> lockedSource, String source, String contracted,
                             String paid, String installments, String notes, List<String> sources) {
    }
}
