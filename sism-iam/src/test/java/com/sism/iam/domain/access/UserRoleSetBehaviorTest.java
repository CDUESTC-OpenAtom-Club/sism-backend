package com.sism.iam.domain.access;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("User Role Set Behavior Tests")
class UserRoleSetBehaviorTest {

    @Test
    @DisplayName("Should keep multiple roles with different ids in a set")
    void shouldKeepMultipleRolesWithDifferentIdsInSet() {
        Role reporter = new Role();
        reporter.setId(5L);
        reporter.setRoleCode("ROLE_REPORTER");

        Role leader = new Role();
        leader.setId(7L);
        leader.setRoleCode("ROLE_LEADER");

        Set<Role> roles = new HashSet<>();
        roles.add(reporter);
        roles.add(leader);

        assertEquals(2, roles.size());
    }
}
