package fr.mtpilates.profile;

import java.util.List;
import java.util.Map;

/** Fiche prête à afficher dans les formulaires (valeurs déjà mises en forme de saisie). */
public record ProfileView(
        String email, String firstName, String lastName, String phone, String dateOfBirth,
        String avatarUrl, String initial, String status,
        String goals, String heightCm, String weightKg, String weeklyTargetSessions, String bio,
        List<String> objectives, List<String> allergies, List<String> pathologies,
        String workSedentarity, String bodyType, String sportHistory, String coachingMotivation, String externalSummary,
        String coachNotes, List<String> equipment, Map<String, List<String>> availability,
        String dietType, List<String> dislikes, List<String> likes, String mealsPerDay, String weeklyBudget, String cookingTime) {

    public boolean hasSlot(final String day, final String slot) {
        return availability.getOrDefault(day, List.of()).contains(slot);
    }
}
