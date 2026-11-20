package org.nmcpye.datarun.jpa.assignment.repository;

import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.common.JpaIdentifiableRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface AssignmentRepository
    extends JpaIdentifiableRepository<Assignment> {

    List<Assignment> findAllByTeamUidIn(Collection<String> teamUIDs);

    /**
     * Live assignments of the given teams that carry at least one of the given
     * forms, with their org units. Disabled teams and activities are left out
     * unless {@code includeDisabled}. A {@code forms} value that is not a JSON
     * array counts as no forms.
     */
    @Query(value = """
        SELECT a.uid AS "uid", o.uid AS "orgUnitUid", o.path AS "orgUnitPath"
        FROM assignment a
        JOIN team t ON t.id = a.team_id
        JOIN activity ac ON ac.id = a.activity_id
        JOIN org_unit o ON o.id = a.org_unit_id
        WHERE t.uid IN (:teamUids)
          AND COALESCE(a.deleted, false) = false
          AND (:includeDisabled
               OR (COALESCE(t.disabled, false) = false AND COALESCE(ac.disabled, false) = false))
          AND EXISTS (
            SELECT 1
            FROM jsonb_array_elements_text(
                CASE WHEN jsonb_typeof(a.forms) = 'array' THEN a.forms ELSE '[]'::jsonb END) AS form(uid)
            WHERE form.uid IN (:formUids))
        """, nativeQuery = true)
    List<ManagedAssignmentRef> findManagedScope(
        @Param("teamUids") Collection<String> teamUids,
        @Param("formUids") Collection<String> formUids,
        @Param("includeDisabled") boolean includeDisabled);

    Page<Assignment> findAllByPathIsNull(Pageable pageable);
}
