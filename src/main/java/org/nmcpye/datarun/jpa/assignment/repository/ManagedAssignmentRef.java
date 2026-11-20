package org.nmcpye.datarun.jpa.assignment.repository;

import java.util.Arrays;
import java.util.List;

/**
 * A managed team's assignment in a manager's scope: its uid and its org unit,
 * read without loading the assignment entity and its eager associations.
 */
public interface ManagedAssignmentRef {
    String getUid();

    String getOrgUnitUid();

    /** The org unit's stored path: root first, the unit itself last. */
    String getOrgUnitPath();

    /** The org unit and its ancestors, from the stored path. */
    default List<String> orgUnitAndAncestorUids() {
        String path = getOrgUnitPath();
        if (path == null || path.isBlank()) {
            return getOrgUnitUid() == null ? List.of() : List.of(getOrgUnitUid());
        }
        return Arrays.stream(path.split(","))
            .filter(uid -> !uid.isBlank())
            .toList();
    }
}
