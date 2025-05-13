package org.nmcpye.datarun.drun.postgres.repository;

import org.nmcpye.datarun.drun.postgres.domain.Assignment;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface AssignmentRepositoryWithBagRelationships {
    void updatePaths();

    void forceUpdatePaths();

    Optional<Assignment> fetchBagRelationships(Assignment assignment);

    Page<Assignment> fetchBagRelationships(Page<Assignment> assignmentPage);

    List<Assignment> fetchBagRelationships(List<Assignment> assignments);
}
