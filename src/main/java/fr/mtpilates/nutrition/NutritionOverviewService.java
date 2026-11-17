package fr.mtpilates.nutrition;

import static fr.mtpilates.nutrition.NutritionAccess.requireStaff;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.member.MemberGuard;
import fr.mtpilates.nutrition.NutritionViews.Overview;
import fr.mtpilates.nutrition.NutritionViews.PlanRow;
import fr.mtpilates.nutrition.NutritionViews.TemplateCard;
import fr.mtpilates.shared.web.ClockConfig;
import fr.mtpilates.user.Profile;
import fr.mtpilates.user.ProfileRepository;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Onglet « Vue d'ensemble » du hub nutrition : compteurs, plans élèves actifs, activité récente. */
@Service
public class NutritionOverviewService {

    private static final DateTimeFormatter FR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int RECENT_TEMPLATES = 2;

    private final RecipeService recipes;
    private final FoodService foods;
    private final MealPlanTemplateService templates;
    private final NutritionPlanRepository plans;
    private final ProfileRepository profiles;
    private final MemberGuard guard;

    public NutritionOverviewService(final RecipeService recipes, final FoodService foods,
                                    final MealPlanTemplateService templates, final NutritionPlanRepository plans,
                                    final ProfileRepository profiles, final MemberGuard guard) {
        this.recipes = recipes;
        this.foods = foods;
        this.templates = templates;
        this.plans = plans;
        this.profiles = profiles;
        this.guard = guard;
    }

    @Transactional(readOnly = true)
    public Overview overview(final CurrentUser user) {
        requireStaff(user);
        final List<TemplateCard> library = templates.library(user);
        return new Overview(recipes.count(), foods.count(user), library.stream().filter(t -> "published".equals(t.status())).count(),
                activePlans(user), library.stream().limit(RECENT_TEMPLATES).toList());
    }

    /** Plans actifs lisibles (policy : membre = moi ‖ gestion du membre), du plus récent au plus ancien. */
    @Transactional(readOnly = true)
    public List<PlanRow> activePlans(final CurrentUser user) {
        final List<NutritionPlan> visible = plans.findByActiveTrueOrderByUpdatedAtDesc().stream()
                .filter(p -> p.getMemberId().equals(user.id()) || guard.canManage(user, p.getMemberId())).toList();
        final Map<UUID, Profile> byId = profiles.findAllById(visible.stream().map(NutritionPlan::getMemberId).toList())
                .stream().collect(Collectors.toMap(Profile::getId, Function.identity()));
        return visible.stream().map(p -> row(p, Optional.ofNullable(byId.get(p.getMemberId())))).toList();
    }

    private static PlanRow row(final NutritionPlan plan, final Optional<Profile> member) {
        final String name = member.map(Profile::displayName).orElse("Élève inconnu");
        final String initial = member.map(m -> m.getFirstName().filter(f -> !f.isEmpty()).orElse(m.getEmail()))
                .filter(s -> !s.isEmpty()).map(s -> s.substring(0, 1)).orElse("?").toUpperCase(Locale.ROOT);
        final String search = (plan.getTitle() + " " + member.flatMap(Profile::getFirstName).orElse("") + " "
                + member.flatMap(Profile::getLastName).orElse("") + " " + member.map(Profile::getEmail).orElse(""))
                .toLowerCase(Locale.ROOT);
        return new PlanRow(plan.getId(), member.map(Profile::getId), name, initial, plan.getTitle(),
                plan.getKcalTarget().filter(k -> k != 0), plan.isActive(), search,
                plan.getUpdatedAt().atZone(ClockConfig.STUDIO_ZONE).format(FR_DATE));
    }
}
