package fr.mtpilates.nutrition;

import static fr.mtpilates.nutrition.NutritionAccess.requireStaff;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.shared.web.FormValues;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Bibliothèque d'aliments (FoodLibrary) et import OpenFoodFacts. */
@Service
public class FoodService {

    private final FoodRepository foods;
    private final OpenFoodFactsClient openFoodFacts;

    public FoodService(final FoodRepository foods, final OpenFoodFactsClient openFoodFacts) {
        this.foods = foods;
        this.openFoodFacts = openFoodFacts;
    }

    @Transactional(readOnly = true)
    public List<FoodRow> list(final CurrentUser user) {
        requireStaff(user);
        return foods.findAllByOrderByNameAsc().stream().map(f -> row(f.getId(), f.content())).toList();
    }

    @Transactional(readOnly = true)
    public long count(final CurrentUser user) {
        requireStaff(user);
        return foods.count();
    }

    @Transactional(readOnly = true)
    public FoodFormValues formValues(final CurrentUser user, final Optional<UUID> id) {
        requireStaff(user);
        return id.flatMap(foods::findById).map(f -> FoodFormValues.of(f.getId(), f.content())).orElseGet(FoodFormValues::empty);
    }

    /** Code-barres absent d'OpenFoodFacts : « Produit introuvable sur OpenFoodFacts ». */
    public Optional<FoodFormValues> lookupBarcode(final CurrentUser user, final String barcode) {
        requireStaff(user);
        final String code = Optional.ofNullable(barcode).map(String::trim).orElse("");
        if (code.isEmpty()) {
            return Optional.empty();
        }
        return openFoodFacts.byBarcode(code).map(OffProduct::toForm)
                .map(f -> f.barcode().isEmpty() ? withBarcode(f, code) : f);
    }

    public List<OffProduct> search(final CurrentUser user, final String query) {
        requireStaff(user);
        return openFoodFacts.search(query.trim());
    }

    @Transactional
    public void save(final CurrentUser user, final Optional<UUID> id, final FoodForm form) {
        requireStaff(user);
        final String name = Optional.ofNullable(form.name()).map(String::trim).orElse("");
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Nom requis");
        }
        final FoodContent content = new FoodContent(name, trimmed(form.brand()), trimmed(form.barcode()),
                number(form.servingSizeG()), number(form.calories()), number(form.protein()), number(form.carbs()),
                number(form.fat()), number(form.fiber()),
                NutritionCatalog.FOOD_SOURCES.containsKey(String.valueOf(form.source())) ? form.source() : "manual",
                NutritionCatalog.FOOD_AISLES.containsKey(String.valueOf(form.aisle())) ? form.aisle() : "autre",
                form.isPublic() != null && form.isPublic().contains("true"));
        final Food food = id.flatMap(foods::findById).orElseGet(() -> new Food(user.id()));
        food.update(content);
        foods.save(food);
    }

    @Transactional
    public void delete(final CurrentUser user, final UUID id) {
        requireStaff(user);
        foods.deleteById(id);
    }

    private static FoodRow row(final UUID id, final FoodContent c) {
        return new FoodRow(id, c.name(), Optional.ofNullable(c.brand()), c.source(), NutritionCatalog.sourceLabel(c.source()),
                c.aisle(), NutritionCatalog.aisleLabel(c.aisle()),
                c.calories() == null ? "—" : FoodFormValues.plain(c.calories()),
                zero(c.protein()) + " / " + zero(c.carbs()) + " / " + zero(c.fat()), Optional.ofNullable(c.barcode()).orElse(""));
    }

    private static String zero(final BigDecimal value) {
        return value == null ? "0" : FoodFormValues.plain(value);
    }

    private static FoodFormValues withBarcode(final FoodFormValues f, final String code) {
        return new FoodFormValues(f.id(), f.name(), f.brand(), code, f.servingSizeG(), f.calories(), f.protein(), f.carbs(),
                f.fat(), f.fiber(), f.source(), f.aisle(), f.isPublic());
    }

    private static String trimmed(final String value) {
        return FormValues.text(value).map(String::trim).orElse(null);
    }

    private static BigDecimal number(final String value) {
        return FormValues.decimal(value).orElse(null);
    }
}
