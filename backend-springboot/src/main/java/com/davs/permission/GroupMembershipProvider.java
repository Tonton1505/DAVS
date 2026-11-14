package com.davs.permission;

import java.util.Set;
import java.util.UUID;

/**
 * Resolves the set of group IDs to which a user belongs for group-level permission evaluation.
 */
public interface GroupMembershipProvider {

    Set<UUID> findGroupIdsForUser(UUID userId);
}
