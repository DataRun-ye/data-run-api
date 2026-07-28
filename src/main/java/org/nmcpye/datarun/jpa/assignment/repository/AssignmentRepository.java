package org.nmcpye.datarun.jpa.assignment.repository;

import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.common.JpaIdentifiableRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface AssignmentRepository
    extends JpaIdentifiableRepository<Assignment> {

    List<Assignment> findAllByTeamUidIn(Collection<String> teamUIDs);

    Page<Assignment> findAllByPathIsNull(Pageable pageable);

    @Modifying
    @Query("update Assignment assignment set assignment.path = :path, assignment.hierarchyLevel = :level where assignment.id = :id")
    int updateDerivedPath(
        @Param("id") String id,
        @Param("path") String path,
        @Param("level") Integer level
    );
}
