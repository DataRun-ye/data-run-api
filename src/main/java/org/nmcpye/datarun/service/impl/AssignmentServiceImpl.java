package org.nmcpye.datarun.service.impl;

import org.nmcpye.datarun.drun.postgres.domain.Assignment;
import org.nmcpye.datarun.repository.AssignmentRepository;
import org.nmcpye.datarun.service.AssignmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service Implementation for managing {@link Assignment}.
 */
@Service
@Transactional
public class AssignmentServiceImpl implements AssignmentService {

    private final Logger log = LoggerFactory.getLogger(AssignmentServiceImpl.class);

    private final AssignmentRepository assignmentRepository;

    public AssignmentServiceImpl(AssignmentRepository assignmentRepository) {
        this.assignmentRepository = assignmentRepository;
    }

    @Override
    public Assignment save(Assignment assignment) {
        log.debug("Request to save Assignment : {}", assignment);
        return assignmentRepository.save(assignment);
    }

    @Override
    public Assignment update(Assignment assignment) {
        log.debug("Request to update Assignment : {}", assignment);
        assignment.setIsPersisted();
        return assignmentRepository.save(assignment);
    }

    @Override
    public Optional<Assignment> partialUpdate(Assignment assignment) {
        log.debug("Request to partially update Assignment : {}", assignment);

        return assignmentRepository
            .findById(assignment.getId())
            .map(existingAssignment -> {
                if (assignment.getUid() != null) {
                    existingAssignment.setUid(assignment.getUid());
                }
                if (assignment.getCode() != null) {
                    existingAssignment.setCode(assignment.getCode());
                }
                if (assignment.getCreatedBy() != null) {
                    existingAssignment.setCreatedBy(assignment.getCreatedBy());
                }
                if (assignment.getCreatedDate() != null) {
                    existingAssignment.setCreatedDate(assignment.getCreatedDate());
                }
                if (assignment.getLastModifiedBy() != null) {
                    existingAssignment.setLastModifiedBy(assignment.getLastModifiedBy());
                }
                if (assignment.getLastModifiedDate() != null) {
                    existingAssignment.setLastModifiedDate(assignment.getLastModifiedDate());
                }

                return existingAssignment;
            })
            .map(assignmentRepository::save);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Assignment> findAll(Pageable pageable) {
        log.debug("Request to get all Assignments");
        return assignmentRepository.findAll(pageable);
    }

    public Page<Assignment> findAllWithEagerRelationships(Pageable pageable) {
        return assignmentRepository.findAllWithEagerRelationships(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Assignment> findOne(Long id) {
        log.debug("Request to get Assignment : {}", id);
        return assignmentRepository.findOneWithEagerRelationships(id);
    }

    @Override
    public void delete(Long id) {
        log.debug("Request to delete Assignment : {}", id);
        assignmentRepository.deleteById(id);
    }
}
