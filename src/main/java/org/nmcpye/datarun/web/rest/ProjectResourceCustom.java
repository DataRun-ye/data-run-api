package org.nmcpye.datarun.web.rest;

import org.nmcpye.datarun.domain.Project;
import org.nmcpye.datarun.repository.ProjectRepositoryCustom;
import org.nmcpye.datarun.service.custom.ProjectServiceCustom;
import org.nmcpye.datarun.web.rest.common.AbstractResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing {@link Project}.
 */
@RestController
@RequestMapping("/api/custom/projects")
public class ProjectResourceCustom extends AbstractResource<Project> {

    private final Logger log = LoggerFactory.getLogger(ProjectResourceCustom.class);

    private final ProjectServiceCustom projectService;

    private final ProjectRepositoryCustom projectRepository;

    public ProjectResourceCustom(ProjectServiceCustom projectService,
                                 ProjectRepositoryCustom projectRepository) {
        this.projectService = projectService;
        this.projectRepository = projectRepository;
    }

    @Override
    protected Page<Project> getList(Pageable pageable, boolean eagerload) {
        return projectService.findAll(pageable);
    }

    @Override
    protected String getName() {
        return "projects";
    }
}
