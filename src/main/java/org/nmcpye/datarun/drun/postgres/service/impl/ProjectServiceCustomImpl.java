package org.nmcpye.datarun.drun.postgres.service.impl;

import org.nmcpye.datarun.domain.Project;
import org.nmcpye.datarun.drun.postgres.repository.ProjectRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.IdentifiableService;
import org.nmcpye.datarun.drun.postgres.service.ProjectServiceCustom;
import org.nmcpye.datarun.service.impl.ProjectServiceImpl;
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
public class ProjectServiceCustomImpl
    extends ProjectServiceImpl
    implements ProjectServiceCustom, IdentifiableService<Project> {
    private final Logger log = LoggerFactory.getLogger(ProjectServiceCustomImpl.class);

    final private ProjectRepositoryCustom projectRepository;

    public ProjectServiceCustomImpl(ProjectRepositoryCustom projectRepository) {
        super(projectRepository);
        this.projectRepository = projectRepository;
    }

    @Override
    public boolean existsByUid(String uid) {
        return projectRepository.findByUid(uid).isPresent();
    }

    @Override
    public Optional<Project> findByUid(String uid) {
        return projectRepository.findByUid(uid);
    }

    @Override
    public void deleteByUid(String uid) {
        projectRepository.deleteByUid(uid);
    }

    @Override
    public Page<Project> findAll(Pageable pageable) {
        return projectRepository.findAll(pageable);
    }

    @Override
    public Page<Project> findAllWithEagerRelationships(Pageable pageable) {
        return projectRepository.findAll(pageable);
    }
}
