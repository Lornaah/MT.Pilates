package fr.mtpilates.prospect;

/** Soumission du dialog lead (dates au format datetime-local, heure de Paris). */
public record ProspectForm(String firstName, String lastName, String email, String phone, String age, String source,
                           String status, String tag, String notes, String objectives, String nextFollowupAt,
                           String appointmentAt) {
}
