package fr.mtpilates.member;

import java.util.List;

/**
 * Enregistrement de la fiche membre (bouton des notes du coach) : résumé et notes pour tout le staff autorisé,
 * statut, abonnement et coach seulement pour un manager, comme l'original.
 */
public record MemberDetailForm(String goals, String coachNotes, List<String> objectives, List<String> allergies,
                               List<String> pathologies, String workSedentarity, String bodyType, String sportHistory,
                               String coachingMotivation, String externalSummary, String status,
                               String subscriptionStartDate, String subscriptionEndDate, String coachId) {
}
