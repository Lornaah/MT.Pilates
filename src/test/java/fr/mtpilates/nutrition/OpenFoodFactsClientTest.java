package fr.mtpilates.nutrition;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class OpenFoodFactsClientTest {

    private final ObjectMapper json = new ObjectMapper();

    @Test
    void normalizesLikeTheEdgeFunction() throws JsonProcessingException {
        final OffProduct product = OpenFoodFactsClient.normalize(json.readTree("""
                {"code":"3017620422003","brands":"Ferrero","serving_quantity":"15",
                 "nutriments":{"energy-kcal_100g":539,"proteins_100g":6.3,"fat_100g":"30.9"}}
                """));

        assertThat(product.name()).isEqualTo("Produit sans nom");
        assertThat(product.barcode()).contains("3017620422003");
        assertThat(product.servingSizeG()).contains(new BigDecimal("15"));
        assertThat(product.calories()).contains(new BigDecimal("539"));
        assertThat(product.protein()).contains(new BigDecimal("6.3"));
        assertThat(product.fat()).isEmpty();
        assertThat(product.imageUrl()).isEmpty();
    }

    @Test
    void zeroServingQuantityCountsAsMissing() throws JsonProcessingException {
        final OffProduct product = OpenFoodFactsClient.normalize(json.readTree("{\"product_name\":\"Eau\",\"serving_quantity\":0}"));

        assertThat(product.servingSizeG()).isEqualTo(Optional.empty());
        assertThat(product.toForm().source()).isEqualTo("open_food_facts");
        assertThat(product.toForm().isPublic()).isTrue();
    }
}
