package fr.mtpilates.nutrition;

import static fr.mtpilates.nutrition.NutritionAccess.requireStaff;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.member.MemberFilters;
import fr.mtpilates.member.MemberGuard;
import fr.mtpilates.member.MemberService;
import fr.mtpilates.nutrition.NutritionViews.MemberOption;
import fr.mtpilates.nutrition.NutritionViews.TemplateCard;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Menus types : bibliothèque et assignation à un élève. */
@Service
public class MealPlanTemplateService {

    static final List<String> DAYS = List.of("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday");
    private static final int MAX_WEEKS = 12;
    private static final int MAX_MEALS = 8;
    private static final int DEFAULT_KCAL = 2000;
    private static final int DEFAULT_PROTEIN = 150;
    private static final int DEFAULT_CARBS = 200;
    private static final int DEFAULT_FAT = 70;

    private final MealPlanTemplateRepository templates;
    private final NutritionPlanRepository plans;
    private final MemberService members;
    private final MemberGuard guard;
    private final Clock clock;
    private final ObjectMapper json;

    public MealPlanTemplateService(final MealPlanTemplateRepository templates, final NutritionPlanRepository plans,
                                   final MemberService members, final MemberGuard guard, final Clock clock,
                                   final ObjectMapper json) {
        this.json = json;
        this.templates = templates;
        this.plans = plans;
        this.members = members;
        this.guard = guard;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<TemplateCard> library(final CurrentUser user) {
        requireStaff(user);
        return templates.findAllByOrderByUpdatedAtDesc().stream().map(MealPlanTemplateService::card).toList();
    }

    @Transactional(readOnly = true)
    public Optional<TemplateCard> card(final CurrentUser user, final UUID id) {
        requireStaff(user);
        return templates.findById(id).map(MealPlanTemplateService::card);
    }

    /** Élèves proposés : tous les profils visibles (useMembers de l'original). */
    @Transactional(readOnly = true)
    public List<MemberOption> assignableMembers(final CurrentUser user) {
        requireStaff(user);
        return members.visibleTo(user, new MemberFilters("", MemberFilters.ALL, MemberFilters.ALL)).stream()
                .map(m -> new MemberOption(m.id(), m.name())).toList();
    }

    /** Copie la semaine choisie dans un nouveau plan actif du membre ; le menu type n'est pas modifié. */
    @Transactional
    public UUID assign(final CurrentUser user, final UUID templateId, final UUID memberId, final int weekIndex) {
        requireStaff(user);
        guard.requireManage(user, memberId);
        final MealPlanTemplate template = templates.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Menu type introuvable"));
        final List<Map<String, Object>> weeks = template.getWeeks();
        if (weekIndex < 0 || weekIndex >= weeks.size()) {
            throw new IllegalArgumentException("Semaine introuvable dans le menu type");
        }
        final NutritionPlan plan = NutritionPlan.fromTemplate(template, days(weeks.get(weekIndex)), memberId, user.id(),
                clock.instant());
        plans.save(plan);
        template.bumpUsage(clock.instant());
        return plan.getId();
    }

    /** État initial de l'éditeur (blankTemplate de l'original, ou le menu existant). */
    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> editorState(final CurrentUser user, final Optional<UUID> id) {
        requireStaff(user);
        if (id.isEmpty()) {
            return Optional.of(blankState());
        }
        return templates.findById(id.get()).map(MealPlanTemplateService::state);
    }

    @Transactional
    public UUID save(final CurrentUser user, final Optional<UUID> id, final String payloadJson, final String status) {
        requireStaff(user);
        final TemplatePayload p = parse(payloadJson);
        final String title = Optional.ofNullable(p.title()).orElse("");
        if (title.trim().isEmpty()) {
            throw new IllegalArgumentException("Titre requis");
        }
        final List<Map<String, Object>> weeks = Optional.ofNullable(p.weeks()).orElse(List.of());
        if (weeks.isEmpty() || weeks.size() > MAX_WEEKS) {
            throw new IllegalArgumentException("Un menu type compte de 1 à 12 semaines");
        }
        final int mealsPerDay = Optional.ofNullable(p.mealsPerDay()).filter(n -> n >= 1 && n <= MAX_MEALS).orElse(3);
        final TemplateContent content = new TemplateContent(title, Optional.ofNullable(p.description()).filter(d -> !d.isEmpty())
                .orElse(null), "published".equals(status) ? "published" : "draft", p.coverColor(), p.kcalTarget(),
                p.macrosTarget(), mealsPerDay, p.dietaryPreferences(), weeks);
        final MealPlanTemplate template = id.flatMap(templates::findById)
                .orElseGet(() -> new MealPlanTemplate(user.id(), clock.instant()));
        template.update(content, clock.instant());
        templates.save(template);
        return template.getId();
    }

    @Transactional
    public void delete(final CurrentUser user, final UUID id) {
        requireStaff(user);
        templates.deleteById(id);
    }

    private TemplatePayload parse(final String payloadJson) {
        try {
            return json.readValue(Optional.ofNullable(payloadJson).orElse("{}"), TemplatePayload.class);
        } catch (final JsonProcessingException e) {
            throw new IllegalArgumentException("Menu type invalide");
        }
    }

    private static Map<String, Object> state(final MealPlanTemplate t) {
        final Map<String, Object> s = new LinkedHashMap<>();
        s.put("title", t.getTitle());
        s.put("description", t.getDescription().orElse(null));
        s.put("status", t.getStatus());
        s.put("cover_color", t.getCoverColor().orElse(null));
        s.put("kcal_target", t.getKcalTarget().orElse(null));
        s.put("macros_target", t.getMacrosTarget().orElse(null));
        s.put("meals_per_day", t.getMealsPerDay());
        s.put("dietary_preferences", t.getDietaryPreferences().orElse(null));
        s.put("duration_weeks", t.getDurationWeeks());
        s.put("weeks", t.getWeeks());
        return s;
    }

    private static Map<String, Object> blankState() {
        final Map<String, Object> s = new LinkedHashMap<>();
        s.put("title", "");
        s.put("description", null);
        s.put("status", "draft");
        s.put("cover_color", "teal");
        s.put("kcal_target", DEFAULT_KCAL);
        s.put("macros_target", Map.of("protein_g", DEFAULT_PROTEIN, "carbs_g", DEFAULT_CARBS, "fat_g", DEFAULT_FAT));
        s.put("meals_per_day", 3);
        s.put("dietary_preferences", Map.of("regime", "omnivore"));
        s.put("duration_weeks", 1);
        s.put("weeks", List.of(emptyWeek(1)));
        return s;
    }

    /** emptyTemplateWeek : les 7 jours présents, sans repas. */
    static Map<String, Object> emptyWeek(final int number) {
        final Map<String, Object> days = new LinkedHashMap<>();
        DAYS.forEach(d -> days.put(d, List.of()));
        final Map<String, Object> week = new LinkedHashMap<>();
        week.put("week_number", number);
        week.put("week_comment", null);
        week.put("days", days);
        return week;
    }

    static TemplateCard card(final MealPlanTemplate t) {
        final String regime = t.getDietaryPreferences().map(p -> p.get("regime")).filter(String.class::isInstance)
                .map(String.class::cast).orElse("omnivore");
        final String search = (t.getTitle() + "\n" + t.getDescription().orElse("")).toLowerCase(Locale.ROOT);
        return new TemplateCard(t.getId(), t.getTitle(), t.getDescription().filter(d -> !d.isEmpty()), t.getStatus(),
                "draft".equals(t.getStatus()), TemplateCovers.of(t.getCoverColor().orElse(null)), t.getUsageCount(),
                t.getDurationWeeks(), t.getKcalTarget().filter(k -> k != 0), t.getMealsPerDay(),
                macrosLine(t.getMacrosTarget()), regime, search, t.getWeeks().size());
    }

    private static String macrosLine(final Optional<Map<String, Object>> macros) {
        return macros.map(m -> "P " + grams(m.get("protein_g")) + "g · G " + grams(m.get("carbs_g")) + "g · L "
                + grams(m.get("fat_g")) + "g").orElse("Macros non définies");
    }

    /** Rendu d'un nombre JSON comme en JavaScript (120 et non 120.0), « — » s'il manque. */
    static String grams(final Object value) {
        if (value instanceof Number n) {
            return new BigDecimal(n.toString()).stripTrailingZeros().toPlainString();
        }
        return value == null ? "—" : value.toString();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> days(final Map<String, Object> week) {
        return week.get("days") instanceof Map<?, ?> d ? (Map<String, Object>) d : Map.of();
    }
}
