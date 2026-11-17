package fr.mtpilates.announcement;

import java.util.List;

/** Formulaire « Nouvelle annonce » / « Modifier l'annonce ». */
public record AnnouncementForm(String title, String body, String coverImageUrl, String channel, String target,
                               List<String> targetUserIds, String targetCoachId, String recurring,
                               String recurrenceFreq, List<String> recurrenceDays, String publishAt) {
}
