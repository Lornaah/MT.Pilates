package fr.mtpilates.prospect;

import java.time.Instant;

/** Champs éditables d'un lead (ProspectFormDialog). */
public record ProspectContent(String firstName, String lastName, String email, String phone, Integer age, String source,
                              String status, String tag, String notes, String objectives, Instant nextFollowupAt,
                              Instant appointmentAt) {
}
