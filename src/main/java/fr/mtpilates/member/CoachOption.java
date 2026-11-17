package fr.mtpilates.member;

import java.util.UUID;

/** Coach proposé dans les filtres et les formulaires d'affectation. */
public record CoachOption(UUID id, String name, String email, String badgeClass) {
}
