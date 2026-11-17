package fr.mtpilates.member;

import java.util.Optional;
import java.util.UUID;

/** Ligne de la liste des membres. */
public record MemberRow(UUID id, String name, String email, Optional<String> avatarUrl, String initial,
                        Optional<String> phone, Optional<String> coachName, String coachBadgeClass, String status,
                        Optional<String> subscription, boolean deletable) {
}
