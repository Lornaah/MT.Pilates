package fr.mtpilates.profile;

import fr.mtpilates.user.Profile;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    /** Vue de formulaire d'une fiche ; appelée par les services uniquement, l'entité ne sort pas de cette couche. */
    public static ProfileView of(final Profile p) {
        final Map<String, Object> diet = p.getDietaryPreferences();
        return new ProfileView(p.getEmail(), p.getFirstName().orElse(""), p.getLastName().orElse(""), p.getPhone().orElse(""),
                p.getDateOfBirth().map(Object::toString).orElse(""), p.getAvatarUrl().orElse(null),
                initialOf(p), p.getStatus().orElse("active"),
                p.getGoals().orElse(""), number(p.getHeightCm()), number(p.getWeightKg()),
                String.valueOf(p.getWeeklyTargetSessions().orElse(2)), p.getBio().orElse(""),
                p.getObjectives(), p.getAllergies(), p.getPathologies(),
                p.getWorkSedentarity().orElse(""), p.getBodyType().orElse(""), p.getSportHistory().orElse(""),
                p.getCoachingMotivation().orElse(""), p.getExternalSummary().orElse(""), p.getCoachNotes().orElse(""),
                p.getAvailableEquipment(), p.getAvailability(),
                String.valueOf(diet.getOrDefault("diet_type", "Omnivore")), stringList(diet.get("dislikes")),
                stringList(diet.get("likes")), String.valueOf(diet.getOrDefault("meals_per_day", 3)),
                Optional.ofNullable(diet.get("weekly_budget")).map(String::valueOf).orElse(""),
                Optional.ofNullable(diet.get("cooking_time")).map(String::valueOf).orElse(""));
    }

    private static String initialOf(final Profile p) {
        final String source = p.getFirstName().filter(s -> !s.isEmpty()).orElse(p.getEmail());
        return source.isEmpty() ? "?" : source.substring(0, 1);
    }

    private static String number(final Optional<BigDecimal> value) {
        return value.map(BigDecimal::stripTrailingZeros).map(BigDecimal::toPlainString).orElse("");
    }

    private static List<String> stringList(final Object value) {
        return value instanceof List<?> items ? items.stream().map(String::valueOf).toList() : List.of();
    }
}
