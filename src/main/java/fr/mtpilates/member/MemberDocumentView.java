package fr.mtpilates.member;

import java.util.Optional;
import java.util.UUID;

/** Ligne de la carte « Documents » d'un membre. */
public record MemberDocumentView(UUID id, String title, Optional<String> category, String uploadedLabel,
                                 String sizeLabel, String url) {
}
