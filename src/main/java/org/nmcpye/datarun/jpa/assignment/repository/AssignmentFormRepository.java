package org.nmcpye.datarun.jpa.assignment.repository;

import org.nmcpye.datarun.jpa.assignment.AssignmentForm;
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
