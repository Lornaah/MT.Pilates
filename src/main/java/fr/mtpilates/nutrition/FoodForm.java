package fr.mtpilates.nutrition;

/** Soumission du dialog aliment ; la case « Visible par tous les coachs » n'envoie « true » que cochée. */
public record FoodForm(String name, String brand, String barcode, String servingSizeG, String calories, String protein,
                       String carbs, String fat, String fiber, String source, String aisle, String isPublic) {
}
