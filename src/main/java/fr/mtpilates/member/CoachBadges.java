package fr.mtpilates.member;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Couleur de badge stable par coach (coachBadgeConfig.ts) : même hachage que l'original pour garder les mêmes couleurs. */
public final class CoachBadges {

    private static final List<String> PALETTE = List.of(
            "bg-coach/15 text-coach",
            "bg-secondary/15 text-secondary",
            "bg-setter/15 text-setter",
            "bg-primary/15 text-primary",
            "bg-sage/20 text-sage-foreground",
            "bg-trial/15 text-trial-foreground",
            "bg-accent/20 text-accent-foreground",
            "bg-bordeaux/15 text-bordeaux");

    private static final String NONE = "bg-muted text-muted-foreground";
    private static final long UINT32 = 0xFFFFFFFFL;

    private CoachBadges() {
    }

    public static String classFor(final Optional<UUID> coachId) {
        return coachId.map(id -> PALETTE.get((int) (hash(id.toString()) % PALETTE.size()))).orElse(NONE);
    }

    /** {@code (hash * 31 + charCode) >>> 0} de l'original : arithmétique sur 32 bits non signés. */
    private static long hash(final String value) {
        long hash = 0;
        for (int i = 0; i < value.length(); i++) {
            hash = (hash * 31 + value.charAt(i)) & UINT32;
        }
        return hash;
    }
}
