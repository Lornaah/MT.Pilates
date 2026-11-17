package fr.mtpilates.video;

import java.util.Optional;

/** Ligne de l'historique des séances vues (fiche membre). */
public record VideoHistoryItem(String title, boolean completed, Optional<Integer> rating, String watchedLabel) {

    public String stars() {
        return rating.map("⭐"::repeat).orElse("");
    }
}
