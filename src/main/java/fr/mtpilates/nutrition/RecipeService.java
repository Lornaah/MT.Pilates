package fr.mtpilates.nutrition;

import static fr.mtpilates.nutrition.NutritionAccess.requireStaff;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.shared.web.FormValues;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Carnet de recettes (RecipeLibrary, RecipeFormDialog). */
@Service
public class RecipeService {

    private static final int VISIBLE_DIETARY_LABELS = 3;

    private final RecipeRepository recipes;
    private final ObjectMapper json;

    public RecipeService(final RecipeRepository recipes, final ObjectMapper json) {
        this.recipes = recipes;
        this.json = json;
    }

    @Transactional(readOnly = true)
    public RecipeLibrary library(final CurrentUser user) {
        final List<RecipeCard> cards = recipes.findAllByOrderByTitleAsc().stream()
                .map(r -> card(r.getId(), r.content(), r.getCreatedBy().filter(user.id()::equals).isPresent())).toList();
        return new RecipeLibrary(cards, cards.stream().filter(RecipeCard::mine).count(),
                cards.stream().filter(c -> c.imageUrl().isPresent()).count());
    }

    /** Recettes proposées dans les grilles de repas (titre, kcal et macros recopiés dans le repas planifié). */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> options() {
        return recipes.findAllByOrderByTitleAsc().stream().map(r -> {
            final RecipeContent c = r.content();
            final Map<String, Object> o = new LinkedHashMap<>();
            o.put("id", r.getId());
            o.put("title", c.title());
            o.put("calories", c.calories());
            o.put("protein_g", c.protein());
            o.put("carbs_g", c.carbs());
            o.put("fat_g", c.fat());
            return o;
        }).toList();
    }

    @Transactional(readOnly = true)
    public long count() {
        return recipes.count();
    }

    /** État initial du dialog (emptyForm / toForm de l'original). */
    @Transactional(readOnly = true)
    public Map<String, Object> formState(final CurrentUser user, final Optional<UUID> id) {
        requireStaff(user);
        return id.flatMap(recipes::findById).map(r -> state(r.content())).orElseGet(RecipeService::emptyState);
    }

    @Transactional
    public void save(final CurrentUser user, final Optional<UUID> id, final String payloadJson) {
        requireStaff(user);
        final RecipePayload p = parse(payloadJson);
        final String title = Optional.ofNullable(p.title()).map(String::trim).orElse("");
        if (title.isEmpty()) {
            throw new IllegalArgumentException("Le titre est requis");
        }
        final List<Ingredient> ingredients = Optional.ofNullable(p.ingredients()).orElse(List.of()).stream()
                .filter(i -> i.name() != null && !i.name().isBlank())
                .map(i -> new Ingredient(i.name().trim(), Optional.ofNullable(i.quantity()).orElse(""),
                        Optional.ofNullable(i.unit()).orElse("g")))
                .toList();
        final RecipeContent content = new RecipeContent(title, trimmed(p.description()),
                Optional.ofNullable(p.category()).filter(c -> !c.isBlank()).orElse("lunch_dinner"),
                FormValues.integer(p.calories()).orElse(null), decimal(p.protein()), decimal(p.carbs()), decimal(p.fat()),
                trimmed(p.instructions()), ingredients, trimmed(p.imageUrl()), FormValues.integer(p.prepMinutes()).orElse(null),
                FormValues.integer(p.cookMinutes()).orElse(null), FormValues.integer(p.servings()).orElse(1),
                known(p.dietaryTags(), NutritionCatalog.DIETARY_TAGS), known(p.allergens(), NutritionCatalog.ALLERGENS),
                known(p.equipment(), NutritionCatalog.EQUIPMENT), known(p.mealTypes(), NutritionCatalog.MEAL_TYPES),
                p.dessert());
        final Recipe recipe = id.flatMap(recipes::findById).orElseGet(() -> new Recipe(user.id()));
        recipe.update(content);
        recipes.save(recipe);
    }

    @Transactional
    public void delete(final CurrentUser user, final UUID id) {
        requireStaff(user);
        recipes.deleteById(id);
    }

    private static RecipeCard card(final UUID id, final RecipeContent c, final boolean mine) {
        final List<String> labels = c.dietaryTags().stream().map(t -> NutritionCatalog.DIETARY_TAGS.getOrDefault(t, t))
                .limit(VISIBLE_DIETARY_LABELS).toList();
        final String search = Stream.concat(Stream.of(c.title(), Optional.ofNullable(c.description()).orElse("")),
                c.ingredients().stream().map(Ingredient::name)).map(s -> s.toLowerCase(Locale.ROOT))
                .reduce((a, b) -> a + "\n" + b).orElse("");
        return new RecipeCard(id, c.title(), Optional.ofNullable(c.description()).filter(d -> !d.isEmpty()), c.category(),
                NutritionCatalog.recipeCategoryLabel(c.category()), Optional.ofNullable(c.calories()),
                orZero(c.prepMinutes()) + orZero(c.cookMinutes()), c.servings(), c.ingredients().size(), labels,
                Math.max(0, c.dietaryTags().size() - VISIBLE_DIETARY_LABELS), grams(c.protein()), grams(c.carbs()),
                grams(c.fat()), Optional.ofNullable(c.imageUrl()).filter(u -> !u.isEmpty()),
                c.title().isEmpty() ? "" : c.title().substring(0, 1).toUpperCase(Locale.ROOT), mine, search,
                c.dietaryTags(), c.allergens(), c.equipment(), c.mealTypes());
    }

    private static Map<String, Object> state(final RecipeContent c) {
        final Map<String, Object> s = new LinkedHashMap<>();
        s.put("title", c.title());
        s.put("description", orEmpty(c.description()));
        s.put("category", Optional.ofNullable(c.category()).orElse("lunch_dinner"));
        s.put("calories", c.calories() == null ? "" : c.calories().toString());
        s.put("protein_g", FoodFormValues.plain(c.protein()));
        s.put("carbs_g", FoodFormValues.plain(c.carbs()));
        s.put("fat_g", FoodFormValues.plain(c.fat()));
        s.put("instructions", orEmpty(c.instructions()));
        s.put("ingredients", c.ingredients().isEmpty() ? List.of(blankLine())
                : c.ingredients().stream().map(Ingredient::toJson).toList());
        s.put("image_url", orEmpty(c.imageUrl()));
        s.put("prep_time_minutes", c.prepMinutes() == null ? "" : c.prepMinutes().toString());
        s.put("cook_time_minutes", c.cookMinutes() == null ? "" : c.cookMinutes().toString());
        s.put("servings", Integer.toString(c.servings()));
        s.put("dietary_tags", c.dietaryTags());
        s.put("allergens", c.allergens());
        s.put("equipment_needed", c.equipment());
        s.put("meal_types", c.mealTypes());
        s.put("is_dessert", c.dessert());
        return s;
    }

    private static Map<String, Object> emptyState() {
        return state(new RecipeContent("", null, "lunch_dinner", null, null, null, null, null, List.of(), null, null, null, 1,
                List.of(), List.of(), List.of(), List.of(), false));
    }

    private static Map<String, Object> blankLine() {
        return new Ingredient("", "", "g").toJson();
    }

    private RecipePayload parse(final String payloadJson) {
        try {
            return json.readValue(Optional.ofNullable(payloadJson).orElse("{}"), RecipePayload.class);
        } catch (final JsonProcessingException e) {
            throw new IllegalArgumentException("Recette invalide");
        }
    }

    private static List<String> known(final List<String> values, final Map<String, String> catalog) {
        return Optional.ofNullable(values).orElse(List.of()).stream().filter(v -> v != null && catalog.containsKey(v))
                .distinct().toList();
    }

    private static Optional<String> grams(final BigDecimal value) {
        return Optional.ofNullable(value).map(FoodFormValues::plain);
    }

    private static BigDecimal decimal(final String value) {
        return FormValues.decimal(value).orElse(null);
    }

    private static String trimmed(final String value) {
        return FormValues.text(value).map(String::trim).orElse(null);
    }

    private static String orEmpty(final String value) {
        return value == null ? "" : value;
    }

    private static int orZero(final Integer value) {
        return value == null ? 0 : value;
    }
}
