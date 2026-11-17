package fr.mtpilates.announcement;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Contenu d'une annonce tel qu'enregistré. */
public record AnnouncementContent(String title, String body, String coverImageUrl, String channel, String target,
                                  List<UUID> targetUserIds, UUID targetCoachId, boolean recurring, String recurrenceRule,
                                  Instant publishAt) {
}
