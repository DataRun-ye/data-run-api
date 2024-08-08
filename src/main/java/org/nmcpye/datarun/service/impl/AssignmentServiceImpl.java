package org.nmcpye.datarun.service.impl;

import java.util.Optional;
import org.nmcpye.datarun.domain.Assignment;
import org.nmcpye.datarun.repository.AssignmentRepository;
import org.nmcpye.datarun.service.AssignmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link org.nmcpye.datarun.domain.Assignment}.
 */
@Service
@Transactional
public class AssignmentServiceImpl implements AssignmentService {

    private static final Logger log = LoggerFactory.getLogger(AssignmentServiceImpl.class);

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
                if (assignment.getName() != null) {
                    existingAssignment.setName(assignment.getName());
                }
                if (assignment.getPhaseNo() != null) {
                    existingAssignment.setPhaseNo(assignment.getPhaseNo());
                }
                if (assignment.getDistrictCode() != null) {
                    existingAssignment.setDistrictCode(assignment.getDistrictCode());
                }
                if (assignment.getGov() != null) {
                    existingAssignment.setGov(assignment.getGov());
                }
                if (assignment.getDistrict() != null) {
                    existingAssignment.setDistrict(assignment.getDistrict());
                }
                if (assignment.getSubdistrict() != null) {
                    existingAssignment.setSubdistrict(assignment.getSubdistrict());
                }
                if (assignment.getVillage() != null) {
                    existingAssignment.setVillage(assignment.getVillage());
                }
                if (assignment.getSubvillage() != null) {
                    existingAssignment.setSubvillage(assignment.getSubvillage());
                }
                if (assignment.getDayId() != null) {
                    existingAssignment.setDayId(assignment.getDayId());
                }
                if (assignment.getPopulation() != null) {
                    existingAssignment.setPopulation(assignment.getPopulation());
                }
                if (assignment.getItnsPlanned() != null) {
                    existingAssignment.setItnsPlanned(assignment.getItnsPlanned());
                }
                if (assignment.getTargetType() != null) {
                    existingAssignment.setTargetType(assignment.getTargetType());
                }
                if (assignment.getLongitude() != null) {
                    existingAssignment.setLongitude(assignment.getLongitude());
                }
                if (assignment.getLatitude() != null) {
                    existingAssignment.setLatitude(assignment.getLatitude());
                }
                if (assignment.getStartDate() != null) {
                    existingAssignment.setStartDate(assignment.getStartDate());
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
