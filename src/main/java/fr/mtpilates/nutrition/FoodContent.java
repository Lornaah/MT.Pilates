package fr.mtpilates.nutrition;

import java.math.BigDecimal;

/** Champs éditables d'un aliment (valeurs nutritionnelles pour 100 g). */
public record FoodContent(String name, String brand, String barcode, BigDecimal servingSizeG, BigDecimal calories,
                          BigDecimal protein, BigDecimal carbs, BigDecimal fat, BigDecimal fiber, String source,
                          String aisle, boolean isPublic) {
}
