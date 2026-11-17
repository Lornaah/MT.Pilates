package fr.mtpilates.finance;

import java.util.List;

/** Filtres et tri du tableau Finance (état de la page de l'original, ici transmis au serveur). */
public record FinanceFilters(String q, String status, String source, String remaining, String sort, String dir) {

    private static final List<String> SORTS = List.of("name", "status", "source", "contracted", "paid", "remaining");

    public String query() {
        return q == null ? "" : q.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public String statusOrAll() {
        return status == null || status.isBlank() ? "all" : status;
    }

    public String sourceOrAll() {
        return source == null || source.isBlank() ? "all" : source;
    }

    public boolean remainingOnly() {
        return "true".equals(remaining);
    }

    public String sortKey() {
        return SORTS.contains(String.valueOf(sort)) ? sort : "remaining";
    }

    public boolean ascending() {
        return "asc".equals(dir);
    }
}
