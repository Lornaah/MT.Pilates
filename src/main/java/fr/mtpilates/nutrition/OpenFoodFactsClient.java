package fr.mtpilates.nutrition;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Accès à OpenFoodFacts (remplace la fonction edge `openfoodfacts`) : recherche par code-barres et par texte.
 * Une erreur réseau ou une réponse en échec donne « aucun produit », comme la fonction d'origine.
 */
@Component
public class OpenFoodFactsClient {

    private static final String SEARCH_FIELDS = "code,product_name,brands,image_thumb_url,serving_quantity,nutriments";

    private final RestClient http;

    public OpenFoodFactsClient(final RestClient.Builder builder,
                               @Value("${app.openfoodfacts.base-url:https://world.openfoodfacts.org}") final String baseUrl) {
        final SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(15));
        // OpenFoodFacts demande un User-Agent identifiant l'application.
        this.http = builder.baseUrl(baseUrl).requestFactory(factory).defaultHeader("User-Agent", "MTPilates/1.0").build();
    }

    public Optional<OffProduct> byBarcode(final String barcode) {
        try {
            final JsonNode body = http.get().uri("/api/v2/product/{code}.json", barcode).retrieve().body(JsonNode.class);
            return Optional.ofNullable(body).filter(b -> b.path("status").asInt() == 1 && b.hasNonNull("product"))
                    .map(b -> normalize(b.get("product")));
        } catch (final RestClientException e) {
            return Optional.empty();
        }
    }

    public List<OffProduct> search(final String query) {
        try {
            final JsonNode body = http.get().uri(u -> u.path("/cgi/search.pl").queryParam("search_terms", query)
                            .queryParam("search_simple", 1).queryParam("action", "process").queryParam("json", 1)
                            .queryParam("page_size", 20).queryParam("page", 1).queryParam("fields", SEARCH_FIELDS).build())
                    .retrieve().body(JsonNode.class);
            if (body == null || !body.path("products").isArray()) {
                return List.of();
            }
            return StreamSupport.stream(body.get("products").spliterator(), false).map(OpenFoodFactsClient::normalize)
                    .filter(p -> p.barcode().isPresent()).toList();
        } catch (final RestClientException e) {
            return List.of();
        }
    }

    static OffProduct normalize(final JsonNode p) {
        final JsonNode n = p.path("nutriments");
        return new OffProduct(text(p, "code"), text(p, "product_name").orElse("Produit sans nom"), text(p, "brands"),
                text(p, "image_thumb_url"), servingQuantity(p.path("serving_quantity")), number(n, "energy-kcal_100g"),
                number(n, "proteins_100g"), number(n, "carbohydrates_100g"), number(n, "fat_100g"), number(n, "fiber_100g"));
    }

    private static Optional<String> text(final JsonNode node, final String field) {
        return Optional.ofNullable(node.get(field)).filter(JsonNode::isTextual).map(JsonNode::asText);
    }

    /** Seuls les nombres JSON comptent : une chaîne dans `nutriments` est ignorée, comme dans la fonction edge. */
    private static Optional<BigDecimal> number(final JsonNode node, final String field) {
        return Optional.ofNullable(node.get(field)).filter(JsonNode::isNumber).map(JsonNode::decimalValue);
    }

    /** `Number(serving_quantity) || null` : nombre ou chaîne numérique, zéro et illisible donnent « absent ». */
    private static Optional<BigDecimal> servingQuantity(final JsonNode node) {
        final Optional<BigDecimal> value;
        if (node.isNumber()) {
            value = Optional.of(node.decimalValue());
        } else if (node.isTextual()) {
            value = fr.mtpilates.shared.web.FormValues.decimal(node.asText());
        } else {
            value = Optional.empty();
        }
        return value.filter(v -> v.signum() != 0);
    }
}
