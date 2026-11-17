package fr.mtpilates.profile;

import java.util.List;

/**
 * Champs de « Ma fiche » et de l'onboarding (l'onboarding y ajoute matériel,
 * disponibilités et préférences alimentaires). Tous facultatifs, comme dans l'original.
 *
 * @param slots disponibilités cochées, au format « jour:créneau » (ex. monday:morning)
 */
public record ProfileForm(
        String firstName, String lastName, String phone, String dateOfBirth,
        String goals, String heightCm, String weightKg, String weeklyTargetSessions, String bio,
        List<String> objectives, List<String> allergies, List<String> pathologies,
        String workSedentarity, String bodyType, String sportHistory, String coachingMotivation, String externalSummary,
        List<String> equipment, List<String> slots,
        String dietType, List<String> dislikes, List<String> likes, String mealsPerDay, String weeklyBudget, String cookingTime) {
}
