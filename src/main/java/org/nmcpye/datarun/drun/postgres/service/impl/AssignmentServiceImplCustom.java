package org.nmcpye.datarun.drun.postgres.service.impl;

import org.nmcpye.datarun.domain.Assignment;
import org.nmcpye.datarun.drun.postgres.repository.AssignmentRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.IdentifiableServiceImpl;
import org.nmcpye.datarun.drun.postgres.service.AssignmentServiceCustom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Primary
@Transactional
public class AssignmentServiceImplCustom
    extends IdentifiableServiceImpl<Assignment>
    implements AssignmentServiceCustom {

    private final Logger log = LoggerFactory.getLogger(AssignmentServiceImplCustom.class);

    AssignmentRepositoryCustom assignmentRepository;

    public AssignmentServiceImplCustom(AssignmentRepositoryCustom assignmentRepository) {
        super(assignmentRepository);
        this.assignmentRepository = assignmentRepository;
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
                if (assignment.getName() != null) {
                    existingAssignment.setName(assignment.getName());
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
}
