package org.nmcpye.datarun.jpa.assignment.repository;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * A managed team's assignment in a manager's scope: its uid and its org unit,
 * read without loading the assignment entity and its eager associations.
 */
public interface ManagedAssignmentRef {
    String getUid();

    String getOrgUnitUid();

    /** The org unit's stored path: root first, the unit itself last. */
    String getOrgUnitPath();

    /** The org unit itself, always, and its ancestors from the stored path. */
    default List<String> orgUnitAndAncestorUids() {
        Stream<String> ancestors = getOrgUnitPath() == null
            ? Stream.empty()
            : Arrays.stream(getOrgUnitPath().split(","));
        return Stream.concat(ancestors, Stream.ofNullable(getOrgUnitUid()))
            .filter(uid -> !uid.isBlank())
            .distinct()
            .toList();
    }
}
