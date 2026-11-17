package fr.mtpilates.member;

import java.util.List;
import java.util.Optional;

/** Documents d'un membre et leurs catégories distinctes (filtre de la carte). */
public record MemberDocuments(List<MemberDocumentView> items, List<String> categories) {

    public static MemberDocuments of(final List<MemberDocumentView> items) {
        return new MemberDocuments(items, items.stream().map(MemberDocumentView::category).flatMap(Optional::stream)
                .distinct().toList());
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
