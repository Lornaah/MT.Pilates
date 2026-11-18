package fr.mtpilates.bilan;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Vues des bilans : liste (fiche membre, espace membre) et éditeur du coach. */
public final class BilanViews {

    private BilanViews() {
    }

    /** Ligne de liste : période, statut, dates, écart de poids. */
    public record Summary(UUID id, UUID memberId, String period, boolean sent, String createdOn, Optional<String> sentOn,
                          Optional<String> weightDelta) {
    }

    /** Photo proposée dans l'éditeur. */
    public record Photo(String id, String url, String side, String takenOn) {
    }

    /** Mensuration proposée : cochable seulement si elle a au moins deux mesures sur la période. */
    public record MetricOption(String key, String label, boolean available, String delta) {
    }

    public record Editor(UUID id, UUID memberId, String memberName, boolean sent, String periodStart, String periodEnd,
                         String targetWeight, String startWeight, String contentJson, List<Photo> photos,
                         List<MetricOption> metrics, Map<String, String> computedDurations) {
    }
}
