package fr.mtpilates.training;

import java.util.Optional;
import java.util.UUID;

/** Carte de la bibliothèque de séances. */
public record SessionCard(UUID id, String title, Optional<String> description, Optional<String> category,
                          String levelCode, Optional<SessionLevel> level, Optional<Integer> durationMinutes,
                          boolean template, int blockCount, int exerciseCount) {
}
