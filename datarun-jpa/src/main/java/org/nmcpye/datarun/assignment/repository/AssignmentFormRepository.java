package org.nmcpye.datarun.assignment.repository;

import org.nmcpye.datarun.drun.postgres.domain.AssignmentForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Hamza Assada, 16/04/2025
 */
@SuppressWarnings("unused")
@Repository
public interface AssignmentFormRepository
    extends JpaRepository<AssignmentForm, Long> {
}
