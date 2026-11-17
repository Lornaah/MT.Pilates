package fr.mtpilates.member;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.user.ProfileRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Applique {@link MemberAccessPolicy} à un membre désigné par son identifiant (coach assigné lu en base). */
@Component
public class MemberGuard {

    private final ProfileRepository profiles;
    private final MemberAccessPolicy policy;

    public MemberGuard(final ProfileRepository profiles, final MemberAccessPolicy policy) {
        this.profiles = profiles;
        this.policy = policy;
    }

    @Transactional(readOnly = true)
    public boolean canManage(final CurrentUser user, final UUID memberId) {
        return policy.canManage(user, memberId, coachOf(memberId));
    }

    public void requireManage(final CurrentUser user, final UUID memberId) {
        if (!canManage(user, memberId)) {
            throw new AccessDeniedException("Membre hors de portée");
        }
    }

    public void requireReadTracking(final CurrentUser user, final UUID memberId) {
        if (!policy.canReadTracking(user, memberId)) {
            throw new AccessDeniedException("Suivi non consultable");
        }
    }

    private Optional<UUID> coachOf(final UUID memberId) {
        return profiles.findById(memberId).flatMap(p -> p.getCoachId());
    }
}
