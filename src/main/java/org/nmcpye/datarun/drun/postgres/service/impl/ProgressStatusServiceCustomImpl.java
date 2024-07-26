package org.nmcpye.datarun.drun.postgres.service.impl;

import org.nmcpye.datarun.domain.ProgressStatus;
import org.nmcpye.datarun.drun.postgres.repository.ProgressStatusRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.IdentifiableService;
import org.nmcpye.datarun.drun.postgres.service.ProgressStatusServiceCustom;
import org.nmcpye.datarun.service.impl.ProgressStatusServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Primary
@Transactional
public class ProgressStatusServiceCustomImpl
    extends ProgressStatusServiceImpl
    implements ProgressStatusServiceCustom, IdentifiableService<ProgressStatus> {

    private final Logger log = LoggerFactory.getLogger(TeamServiceCustomImpl.class);

    ProgressStatusRepositoryCustom progressStatusRepository;

    public ProgressStatusServiceCustomImpl(ProgressStatusRepositoryCustom progressStatusRepository) {
        super(progressStatusRepository);
        this.progressStatusRepository = progressStatusRepository;
    }

    @Override
    public boolean existsByUid(String uid) {
        return progressStatusRepository.findByUid(uid).isPresent();
    }

    @Override
    public Optional<ProgressStatus> findByUid(String uid) {
        return progressStatusRepository.findByUid(uid);
    }

    @Override
    public void deleteByUid(String uid) {
        progressStatusRepository.deleteByUid(uid);
    }

    @Override
    public Page<ProgressStatus> findAll(Pageable pageable) {
        return progressStatusRepository.findAll(pageable);
    }

    @Override
    public Page<ProgressStatus> findAllWithEagerRelationships(Pageable pageable) {
        return progressStatusRepository.findAll(pageable);
    }

    @Override
    public List<ProgressStatus> findAll() {
        return progressStatusRepository.findAll();
    }

}
