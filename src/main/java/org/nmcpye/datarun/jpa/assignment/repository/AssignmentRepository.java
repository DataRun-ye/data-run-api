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
     * Assignments of the given teams that carry at least one of the given forms.
     */
    @Query(value = """
        SELECT a.* FROM assignment a
        JOIN team t ON t.id = a.team_id
        WHERE t.uid IN (:teamUids)
          AND EXISTS (
            SELECT 1
            FROM jsonb_array_elements_text(COALESCE(a.forms, '[]'::jsonb)) AS form(uid)
            WHERE form.uid IN (:formUids))
        """, nativeQuery = true)
    List<Assignment> findAllByTeamUidInCarryingAnyForm(
        @Param("teamUids") Collection<String> teamUids,
        @Param("formUids") Collection<String> formUids);

    Page<Assignment> findAllByPathIsNull(Pageable pageable);
}
