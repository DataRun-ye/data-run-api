package org.nmcpye.datarun.service.custom.impl;

import org.nmcpye.datarun.domain.Project;
import org.nmcpye.datarun.repository.ProjectRepositoryCustom;
import org.nmcpye.datarun.service.custom.ProjectServiceCustom;
import org.nmcpye.datarun.service.impl.ProjectServiceImpl;
import org.nmcpye.datarun.utils.CodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Primary
@Transactional
public class ProjectServiceCustomImpl
    extends ProjectServiceImpl
    implements ProjectServiceCustom {
    private final Logger log = LoggerFactory.getLogger(ItnsVillageServiceCustomImpl.class);

    final private ProjectRepositoryCustom projectRepository;

    public ProjectServiceCustomImpl(ProjectRepositoryCustom projectRepository) {
        super(projectRepository);
        this.projectRepository = projectRepository;
    }

    @Override
    public Project save(Project project) {
        if (project.getUid() == null || project.getUid().isEmpty()) {
            project.setUid(CodeGenerator.generateUid());
        }
        return projectRepository.save(project);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Project> findAll(Pageable pageable) {
        log.debug("Request to get all projects");
        return projectRepository.findAll(pageable);
    }
}
