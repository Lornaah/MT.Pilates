package fr.mtpilates.member;

/** Champs administratifs d'un membre (EditMemberDialog) ; dates au format yyyy-MM-dd, vides = aucune. */
public record EditMemberForm(String status, String subscriptionStartDate, String subscriptionEndDate, String phone,
                             String coachId) {
}
