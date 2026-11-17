package fr.mtpilates.nutrition;

import java.util.Optional;
import java.util.UUID;

/** Ligne du tableau des aliments. */
public record FoodRow(UUID id, String name, Optional<String> brand, String source, String sourceLabel, String aisle,
                      String aisleLabel, String calories, String macros, String barcode) {
}
