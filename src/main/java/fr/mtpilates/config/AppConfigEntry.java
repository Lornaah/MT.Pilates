package fr.mtpilates.config;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** Paire clé/valeur de configuration (libellés surchargeables par l'owner). */
@Entity
@Table(name = "app_config")
public class AppConfigEntry {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String key;

    @Column(nullable = false)
    private String value;

    protected AppConfigEntry() {
    }

    public AppConfigEntry(final String key, final String value) {
        this.id = UUID.randomUUID();
        this.key = key;
        this.value = value;
    }

    public void changeValue(final String newValue) {
        this.value = newValue;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
