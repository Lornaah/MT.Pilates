package fr.mtpilates.user;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Listes de choix du profil (MemberSummaryForm, SportStep, NutritionStep, EQUIPMENT_SUGGESTIONS). */
public final class ProfileOptions {

    public static final Map<String, String> SEDENTARITY = ordered(
            "sedentary", "Sédentaire", "light_activity", "Activité légère", "active", "Actif·ve", "very_active", "Très actif·ve");

    public static final Map<String, String> BODY_TYPES = ordered(
            "ectomorph", "Ectomorphe", "endomorph", "Endomorphe", "mesomorph", "Mésomorphe", "mixed", "Mixte");

    public static final List<String> EQUIPMENT = List.of(
            "Aucun (poids du corps)", "Haltères", "Barre", "Kettlebell", "Élastiques",
            "TRX / Sangles", "Banc", "Tapis", "Swiss ball", "Medicine ball",
            "Poulie", "Machine guidée", "Smith machine", "Box / Step", "Corde à sauter",
            "Vélo", "Rameur", "Tapis de course", "Barre de traction", "Anneaux");

    public static final Map<String, String> DAYS = ordered(
            "monday", "Lun", "tuesday", "Mar", "wednesday", "Mer", "thursday", "Jeu",
            "friday", "Ven", "saturday", "Sam", "sunday", "Dim");

    public static final Map<String, String> SLOTS = ordered("morning", "Matin", "noon", "Midi", "evening", "Soir");

    public static final List<String> DIET_TYPES = List.of(
            "Omnivore", "Végétarien", "Végan", "Pescétarien", "Sans gluten", "Sans lactose", "Cétogène", "Paléo");

    public static final Map<String, String> COOKING_TIMES = ordered(
            "quick", "Rapide (< 15 min)", "medium", "Moyen (15–30 min)", "long", "Long (> 30 min)");

    private ProfileOptions() {
    }

    public static String sedentarityLabel(final String value) {
        return value == null ? "—" : SEDENTARITY.getOrDefault(value, "—");
    }

    public static String bodyTypeLabel(final String value) {
        return value == null ? "—" : BODY_TYPES.getOrDefault(value, "—");
    }

    private static Map<String, String> ordered(final String... pairs) {
        final Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return Collections.unmodifiableMap(map);
    }
}
