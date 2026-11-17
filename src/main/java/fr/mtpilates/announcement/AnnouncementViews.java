package fr.mtpilates.announcement;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Vues des annonces. */
public final class AnnouncementViews {

    private AnnouncementViews() {
    }

    public record StaffItem(UUID id, String title, String channel, String target, boolean recurring, String publishedLabel,
                            String bodyHtml, Optional<String> coverUrl) {
    }

    public record StaffGroups(List<StaffItem> drafts, List<StaffItem> scheduled, List<StaffItem> published) {
    }

    public record MemberItem(UUID id, String title, String dateLabel, String bodyHtml, String excerptHtml,
                             String relative, Optional<String> coverUrl, boolean unread) {
    }

    public record Bell(int unread, List<MemberItem> latest) {

        public String unreadBadge() {
            return unread > 9 ? "9+" : String.valueOf(unread);
        }
    }

    /** Valeurs du formulaire (publishAt au format datetime-local, heure de Paris). */
    public record FormValues(Optional<UUID> id, String title, String body, String coverUrl, String channel, String target,
                             List<UUID> targetUserIds, String targetCoachId, boolean recurring, String recurrenceFreq,
                             List<String> recurrenceDays, String publishAt) {
    }

    public record Option(UUID id, String label, String email) {
    }
}
