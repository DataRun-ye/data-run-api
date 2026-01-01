package org.nmcpye.datarun.party.repository;

import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.nmcpye.datarun.party.entities.AssignmentMember;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssignmentMemberRepository extends BaseJpaRepository<AssignmentMember, Long> {
    List<AssignmentMember> findByAssignmentId(String assignmentId);
}
