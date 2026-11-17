package fr.mtpilates.member;

import static org.assertj.core.api.Assertions.assertThat;

import fr.mtpilates.auth.Capacities;
import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.user.AppRole;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MemberAccessPolicyTest {

    private final MemberAccessPolicy policy = new MemberAccessPolicy();
    private final UUID memberId = UUID.randomUUID();

    @Test
    void memberSeesOnlyTheirOwnProfile() {
        final CurrentUser member = user(AppRole.MEMBER);
        assertThat(policy.canView(member, member.id(), Optional.empty())).isTrue();
        assertThat(policy.canView(member, memberId, Optional.empty())).isFalse();
    }

    @Test
    void coachSeesAndUpdatesOnlyTheMembersAssignedToThem() {
        final CurrentUser coach = user(AppRole.COACH);
        assertThat(policy.canUpdate(coach, memberId, Optional.of(coach.id()))).isTrue();
        assertThat(policy.canView(coach, memberId, Optional.of(UUID.randomUUID()))).isFalse();
        assertThat(policy.canCreate(coach)).isFalse();
        assertThat(policy.canDelete(coach, memberId, false)).isFalse();
    }

    @Test
    void managementSeesEveryoneAndManagesAccounts() {
        final CurrentUser setter = user(AppRole.SETTER);
        assertThat(policy.canView(setter, memberId, Optional.empty())).isTrue();
        assertThat(policy.canCreate(setter)).isTrue();
        assertThat(policy.canDelete(setter, memberId, false)).isTrue();
    }

    @Test
    void neverDeletesOneselfOrAnOwner() {
        final CurrentUser owner = user(AppRole.OWNER);
        assertThat(policy.canDelete(owner, owner.id(), true)).isFalse();
        assertThat(policy.canDelete(owner, memberId, true)).isFalse();
    }

    @Test
    void viewAsDoesNotGrantDataAccess() {
        final UUID id = UUID.randomUUID();
        final CurrentUser ownerViewingAsMember = new CurrentUser(id, "o@example.com", Optional.empty(),
                Set.of(AppRole.OWNER), Set.of(AppRole.MEMBER), Optional.of(AppRole.MEMBER), Capacities.of(Set.of(AppRole.MEMBER)));
        assertThat(policy.canView(ownerViewingAsMember, memberId, Optional.empty())).isTrue();
    }

    private static CurrentUser user(final AppRole role) {
        return new CurrentUser(UUID.randomUUID(), role.code() + "@example.com", Optional.empty(), Set.of(role), Set.of(role),
                Optional.empty(), Capacities.of(Set.of(role)));
    }
}
