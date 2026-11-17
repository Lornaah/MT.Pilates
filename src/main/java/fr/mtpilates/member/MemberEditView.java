package fr.mtpilates.member;

import java.util.UUID;

/** Valeurs actuelles pré-remplies dans le dialog de modification. */
public record MemberEditView(UUID id, String title, String status, String subscriptionStartDate,
                             String subscriptionEndDate, String phone, String coachId) {
}
