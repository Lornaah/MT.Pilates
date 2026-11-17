package fr.mtpilates.nutrition;

import fr.mtpilates.auth.CurrentUserService;
import fr.mtpilates.shared.web.Toast;
import fr.mtpilates.shared.web.ToastResponses;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Aliments : dialogs, import OpenFoodFacts (code-barres, recherche) et enregistrement. */
@Controller
@RequestMapping("/app/coach/nutrition/foods")
public class FoodController {

    private static final String VIEW = "fragments/nutrition/foods";
    private static final String TABLE = VIEW + " :: table";
    private static final String FORM = VIEW + " :: foodDialog";
    private static final int MIN_QUERY = 2;

    private final FoodService foods;
    private final CurrentUserService currentUsers;
    private final ToastResponses toasts;

    public FoodController(final FoodService foods, final CurrentUserService currentUsers, final ToastResponses toasts) {
        this.foods = foods;
        this.currentUsers = currentUsers;
        this.toasts = toasts;
    }

    @GetMapping({"/new", "/{id}/edit"})
    public String form(@PathVariable(required = false) final UUID id, final Model model) {
        return form(model, foods.formValues(currentUsers.require(), Optional.ofNullable(id)));
    }

    @PostMapping("/prefill")
    public String prefill(@ModelAttribute final FoodForm form, final Model model) {
        return form(model, FoodFormValues.imported(form));
    }

    @GetMapping("/barcode")
    public String barcodeDialog() {
        return VIEW + " :: barcodeDialog";
    }

    @PostMapping("/barcode")
    public String barcode(@RequestParam(defaultValue = "") final String barcode, final HttpServletResponse response,
                          final Model model) {
        if (barcode.isBlank()) {
            response.setHeader("HX-Reswap", "none");
            return barcodeDialog();
        }
        final Optional<FoodFormValues> found = foods.lookupBarcode(currentUsers.require(), barcode);
        if (found.isEmpty()) {
            toasts.trigger(response, Toast.error("Produit introuvable sur OpenFoodFacts"));
            response.setHeader("HX-Reswap", "none");
            return barcodeDialog();
        }
        return form(model, found.get());
    }

    @GetMapping("/search")
    public String searchDialog() {
        return VIEW + " :: searchDialog";
    }

    @PostMapping("/search")
    public String search(@RequestParam(defaultValue = "") final String q, final HttpServletResponse response,
                         final Model model) {
        if (q.trim().length() < MIN_QUERY) {
            toasts.trigger(response, Toast.error("Saisis au moins 2 caractères"));
            response.setHeader("HX-Reswap", "none");
            model.addAttribute("results", List.of());
            return VIEW + " :: searchResults";
        }
        final List<OffProduct> results = foods.search(currentUsers.require(), q);
        if (results.isEmpty()) {
            toasts.trigger(response, Toast.info("Aucun résultat"));
        }
        model.addAttribute("results", results);
        return VIEW + " :: searchResults";
    }

    @PostMapping({"", "/{id}"})
    public String save(@PathVariable(required = false) final UUID id, @ModelAttribute final FoodForm form,
                       final HttpServletResponse response, final Model model) {
        try {
            foods.save(currentUsers.require(), Optional.ofNullable(id), form);
            toasts.trigger(response, Toast.success(id == null ? "Aliment ajouté" : "Aliment mis à jour"),
                    Map.of("dialog-close", "food-form"));
        } catch (final IllegalArgumentException e) {
            toasts.trigger(response, Toast.error(e.getMessage()));
            response.setHeader("HX-Reswap", "none");
        }
        return table(model);
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable final UUID id, final HttpServletResponse response, final Model model) {
        foods.delete(currentUsers.require(), id);
        toasts.trigger(response, Toast.success("Aliment supprimé"));
        return table(model);
    }

    private String form(final Model model, final FoodFormValues values) {
        model.addAttribute("food", values);
        model.addAttribute("foodSources", NutritionCatalog.FOOD_SOURCES);
        model.addAttribute("foodAisles", NutritionCatalog.FOOD_AISLES);
        return FORM;
    }

    private String table(final Model model) {
        NutritionHubController.fillFoods(model, foods.list(currentUsers.require()));
        return TABLE;
    }
}
