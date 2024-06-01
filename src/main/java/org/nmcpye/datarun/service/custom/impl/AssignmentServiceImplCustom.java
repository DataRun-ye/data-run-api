package org.nmcpye.datarun.service.custom.impl;

import org.nmcpye.datarun.domain.Assignment;
import org.nmcpye.datarun.repository.AssignmentRepositoryCustom;
import org.nmcpye.datarun.service.custom.AssignmentServiceCustom;
import org.nmcpye.datarun.service.impl.AssignmentServiceImpl;
import org.nmcpye.datarun.utils.CodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Primary
@Transactional
public class AssignmentServiceImplCustom
    extends AssignmentServiceImpl implements AssignmentServiceCustom {

    private final Logger log = LoggerFactory.getLogger(AssignmentServiceImplCustom.class);

    AssignmentRepositoryCustom assignmentRepository;

    public AssignmentServiceImplCustom(AssignmentRepositoryCustom assignmentRepository) {
        super(assignmentRepository);
        this.assignmentRepository = assignmentRepository;
    }

    @Override
    public Assignment save(Assignment assignment) {
        if (assignment.getUid() == null || assignment.getUid().isEmpty()) {
            assignment.setUid(CodeGenerator.generateUid());
        }
        return super.save(assignment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Assignment> findAll(Pageable pageable) {
        log.debug("Request to get all Assignments");
        return assignmentRepository.findAllByUser(pageable);
    }

    public Page<Assignment> findAllWithEagerRelationships(Pageable pageable) {
        return assignmentRepository.findAllWithEagerRelationshipsByUser(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Assignment> findOne(Long id) {
        log.debug("Request to get Assignment : {}", id);
        return assignmentRepository.findOneWithEagerRelationshipsByUser(id);
    }
}
