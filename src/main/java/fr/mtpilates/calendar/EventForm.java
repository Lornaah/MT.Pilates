package fr.mtpilates.calendar;

/** « Nouvel événement » : date et heure au format yyyy-MM-ddTHH:mm (heure de Paris). */
public record EventForm(String title, String type, String startsAt, String memberId, String description) {
}
