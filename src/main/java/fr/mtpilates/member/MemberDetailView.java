package fr.mtpilates.member;

import fr.mtpilates.profile.ProfileView;
import java.util.UUID;

/** Fiche membre vue par le staff : en-tête, résumé, notes, statut ; `profile` alimente le formulaire de résumé. */
public record MemberDetailView(UUID id, String displayName, String email, String avatarUrl, String initial,
                               String status, String coachId, String subscriptionStartDate,
                               String subscriptionEndDate, String sedentarityLabel, String bodyTypeLabel,
                               ProfileView profile) {
}
