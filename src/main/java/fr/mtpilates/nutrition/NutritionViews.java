package fr.mtpilates.nutrition;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Vues du hub nutrition. */
public final class NutritionViews {

    private NutritionViews() {
    }

    /** Carte de menu type (MealPlanTemplateCard) ; `regime` et `search` alimentent les filtres côté navigateur. */
    public record TemplateCard(UUID id, String title, Optional<String> description, String status, boolean draft,
                               TemplateCovers.Cover cover, int usageCount, int durationWeeks, Optional<Integer> kcalTarget,
                               int mealsPerDay, String macrosLine, String regime, String search, int weekCount) {
    }

    /** Ligne de MemberPlansList. */
    public record PlanRow(UUID id, Optional<UUID> memberId, String memberName, String initial, String title,
                          Optional<Integer> kcalTarget, boolean active, String search, String updatedOn) {
    }

    public record Overview(long recipes, long foods, long publishedTemplates, List<PlanRow> plans,
                           List<TemplateCard> recentTemplates) {

        public boolean hasActivity() {
            return !plans.isEmpty() || !recentTemplates.isEmpty();
        }
    }

    /** Élève proposé dans le dialog « Assigner ». */
    public record MemberOption(UUID id, String name) {
    }
}
