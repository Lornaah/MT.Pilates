package fr.mtpilates.nutrition;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.auth.CurrentUserService;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Hub nutrition (/app/coach/nutrition?tab=overview|recipes|foods|templates) et redirections de l'original. */
@Controller
public class NutritionHubController {

    private static final List<String> TABS = List.of("overview", "recipes", "foods", "templates");

    /** En-tête de page propre à chaque onglet (HEADERS de NutritionHub). */
    public record Header(String eyebrow, String title, String description) {
    }

    private static final Map<String, Header> HEADERS = Map.of(
            "overview", new Header("Coach", "Nutrition", "Bibliothèques, menus types et plans personnalisés."),
            "recipes", new Header("Nutrition", "Carnet de recettes", "Vos recettes alimentent les plans repas générés pour vos élèves."),
            "foods", new Header("Nutrition", "Bibliothèque d'aliments", "Référentiel utilisé par les recettes et le calcul de macros."),
            "templates", new Header("Nutrition", "Menus types", "Créés une fois, assignés à plusieurs élèves — comme les programmes sportifs."));

    private final NutritionOverviewService overview;
    private final RecipeService recipes;
    private final FoodService foods;
    private final MealPlanTemplateService templates;
    private final CurrentUserService currentUsers;

    public NutritionHubController(final NutritionOverviewService overview, final RecipeService recipes,
                                  final FoodService foods, final MealPlanTemplateService templates,
                                  final CurrentUserService currentUsers) {
        this.overview = overview;
        this.recipes = recipes;
        this.foods = foods;
        this.templates = templates;
        this.currentUsers = currentUsers;
    }

    @GetMapping("/app/coach/nutrition")
    public String hub(@RequestParam(defaultValue = "overview") final String tab, final Model model) {
        final String active = TABS.contains(tab) ? tab : "overview";
        final CurrentUser user = currentUsers.require();
        model.addAttribute("tab", active);
        model.addAttribute("header", HEADERS.get(active));
        switch (active) {
            case "recipes" -> fillRecipes(model, recipes.library(user));
            case "foods" -> fillFoods(model, foods.list(user));
            case "templates" -> fillTemplates(model, templates.library(user));
            default -> model.addAttribute("overview", overview.overview(user));
        }
        return "pages/nutrition";
    }

    @GetMapping("/app/coach/recipes")
    public String recipesRedirect() {
        return "redirect:/app/coach/nutrition?tab=recipes";
    }

    @GetMapping("/app/coach/foods")
    public String foodsRedirect() {
        return "redirect:/app/coach/nutrition?tab=foods";
    }

    static void fillRecipes(final Model model, final RecipeLibrary library) {
        model.addAttribute("library", library);
        model.addAttribute("recipeCategories", NutritionCatalog.RECIPE_CATEGORIES);
        model.addAttribute("dietaryTags", NutritionCatalog.DIETARY_TAGS);
        model.addAttribute("allergens", NutritionCatalog.ALLERGENS);
        model.addAttribute("equipment", NutritionCatalog.EQUIPMENT);
        model.addAttribute("mealTypes", NutritionCatalog.MEAL_TYPES);
        model.addAttribute("units", NutritionCatalog.UNITS);
    }

    static void fillFoods(final Model model, final List<FoodRow> rows) {
        model.addAttribute("foods", rows);
        model.addAttribute("foodSources", NutritionCatalog.FOOD_SOURCES);
        model.addAttribute("foodAisles", NutritionCatalog.FOOD_AISLES);
    }

    static void fillTemplates(final Model model, final List<NutritionViews.TemplateCard> cards) {
        model.addAttribute("templates", cards);
        model.addAttribute("regimes", NutritionCatalog.REGIMES);
    }
}
