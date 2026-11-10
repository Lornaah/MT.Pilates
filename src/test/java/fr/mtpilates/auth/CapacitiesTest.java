package fr.mtpilates.auth;

import static org.assertj.core.api.Assertions.assertThat;

import fr.mtpilates.user.AppRole;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CapacitiesTest {

    @Test
    void ownerIsASuperRoleGrantingEveryStaffCapacity() {
        final Capacities caps = Capacities.of(EnumSet.of(AppRole.OWNER));

        assertThat(caps.isOwner()).isTrue();
        assertThat(caps.isCoach()).isTrue();
        assertThat(caps.isSetter()).isTrue();
        assertThat(caps.isCloser()).isTrue();
        assertThat(caps.isManager()).isTrue();
        assertThat(caps.canManageCoaches()).isTrue();
        assertThat(caps.isStaff()).isTrue();
        assertThat(caps.isMember()).isFalse();
    }

    @Test
    void userWithoutRoleIsTreatedAsMember() {
        final Capacities caps = Capacities.of(Set.of());

        assertThat(caps.isMember()).isTrue();
        assertThat(caps.isStaff()).isFalse();
    }

    @Test
    void closerAndManagersAreStaffForTheInterface() {
        assertThat(Capacities.of(EnumSet.of(AppRole.CLOSER)).isStaff()).isTrue();
        assertThat(Capacities.of(EnumSet.of(AppRole.MANAGER_SETTER)).isStaff()).isTrue();
        assertThat(Capacities.of(EnumSet.of(AppRole.MANAGER_SETTER)).canManageSetters()).isTrue();
    }

    @Test
    void externalMemberIsNotARegularMember() {
        final Capacities caps = Capacities.of(EnumSet.of(AppRole.EXTERNAL_MEMBER));

        assertThat(caps.isExternalMember()).isTrue();
        assertThat(caps.isMember()).isFalse();
    }

    @Test
    void historicalAdminAndUserRolesGrantNothing() {
        final Capacities caps = Capacities.of(EnumSet.of(AppRole.ADMIN, AppRole.USER));

        assertThat(caps.isStaff()).isFalse();
        assertThat(caps.isMember()).isTrue();
    }

    @Test
    void dataAccessFollowsTheOriginalDatabaseFunctions() {
        assertThat(DataAccessRoles.isStaff(EnumSet.of(AppRole.SETTER))).isTrue();
        assertThat(DataAccessRoles.isStaff(EnumSet.of(AppRole.CLOSER))).isFalse();
        assertThat(DataAccessRoles.isStaff(EnumSet.of(AppRole.MANAGER_COACH))).isFalse();
        assertThat(DataAccessRoles.isManagement(EnumSet.of(AppRole.SETTER))).isTrue();
        assertThat(DataAccessRoles.isManagement(EnumSet.of(AppRole.COACH))).isFalse();
    }
}
