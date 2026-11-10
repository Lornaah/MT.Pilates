package fr.mtpilates.shared.ui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * Sérialise des données pour les composants Alpine et Chart.js
 * ({@code ${json.write(valeur)}} dans les templates).
 */
@Component
public class JsonWriter {

    private final ObjectMapper objectMapper;

    public JsonWriter(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String write(final Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (final JsonProcessingException e) {
            throw new IllegalArgumentException("Valeur non sérialisable en JSON", e);
        }
    }
}
