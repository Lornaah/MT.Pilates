package fr.mtpilates.training;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Carte de la bibliothèque de planifications. */
public record ProgramCard(UUID id, String title, Optional<String> description, ProgramStatus status,
                          List<String> objectives, String assigneeLabel, String period) {
}
