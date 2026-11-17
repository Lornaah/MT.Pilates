package fr.mtpilates.prospect;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Vues des pages Leads et Closing. */
public final class ProspectViews {

    private ProspectViews() {
    }

    /** Lead affiché (tableau, pipeline, closing) ; les dates sont déjà formatées à la française. */
    public record Row(UUID id, String name, String firstName, Optional<UUID> memberId, Optional<String> email,
                      Optional<String> phone, Optional<String> tag, Optional<String> notes, Optional<String> objectives,
                      Optional<Integer> age, String source, String status, String createdOn,
                      Optional<java.time.Instant> appointmentAt, Optional<String> appointment, Optional<String> followup,
                      boolean convertible, String search,
                      Optional<String> reminderWhatsapp, Optional<String> closingWhatsapp) {
    }

    public record Stats(int total, int toCall, int upcoming, int converted) {
    }

    public record Column(String status, List<Row> prospects) {
    }

    /** Ligne de la page Closing, avec la date à afficher (RDV ou relance). */
    public record CallRow(Row prospect, Optional<String> when) {
    }

    public record Calls(List<CallRow> upcoming, List<CallRow> toCall, List<CallRow> trial) {
    }

    /** Valeurs du dialog « Nouveau prospect » / « Modifier le prospect » (texte brut). */
    public record FormValues(Optional<UUID> id, String firstName, String lastName, String email, String phone, String age,
                             String source, String status, String tag, String notes, String objectives,
                             String nextFollowupAt, String appointmentAt) {
    }
}
