package org.nmcpye.datarun.service.impl;

import java.util.Optional;
import org.nmcpye.datarun.domain.Project;
import org.nmcpye.datarun.repository.ProjectRepository;
import org.nmcpye.datarun.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link org.nmcpye.datarun.domain.Project}.
 */
@Service
@Transactional
public class ProjectServiceImpl implements ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectServiceImpl.class);

    private final ProjectRepository projectRepository;

    public ProjectServiceImpl(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Override
    public Project save(Project project) {
        log.debug("Request to save Project : {}", project);
        return projectRepository.save(project);
    }

    @Override
    public Project update(Project project) {
        log.debug("Request to update Project : {}", project);
        project.setIsPersisted();
        return projectRepository.save(project);
    }

    @Override
    public Optional<Project> partialUpdate(Project project) {
        log.debug("Request to partially update Project : {}", project);

        return projectRepository
            .findById(project.getId())
            .map(existingProject -> {
                if (project.getUid() != null) {
                    existingProject.setUid(project.getUid());
                }
                if (project.getCode() != null) {
                    existingProject.setCode(project.getCode());
                }
                if (project.getName() != null) {
                    existingProject.setName(project.getName());
                }
                if (project.getDisabled() != null) {
                    existingProject.setDisabled(project.getDisabled());
                }
                if (project.getCreatedBy() != null) {
                    existingProject.setCreatedBy(project.getCreatedBy());
                }
                if (project.getCreatedDate() != null) {
                    existingProject.setCreatedDate(project.getCreatedDate());
                }
                if (project.getLastModifiedBy() != null) {
                    existingProject.setLastModifiedBy(project.getLastModifiedBy());
                }
                if (project.getLastModifiedDate() != null) {
                    existingProject.setLastModifiedDate(project.getLastModifiedDate());
                }

                return existingProject;
            })
            .map(projectRepository::save);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Project> findAll(Pageable pageable) {
        log.debug("Request to get all Projects");
        return projectRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Project> findOne(Long id) {
        log.debug("Request to get Project : {}", id);
        return projectRepository.findById(id);
    }

    @Override
    public void delete(Long id) {
        log.debug("Request to delete Project : {}", id);
        projectRepository.deleteById(id);
    }
}
