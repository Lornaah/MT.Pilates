package fr.mtpilates.video;

/** « Nouvelle séance » : titre et URL ou identifiant YouTube obligatoires. */
public record VideoForm(String title, String youtube, String duration, String level, String focus,
                        String description) {
}
