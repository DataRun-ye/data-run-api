package org.nmcpye.datarun.web.rest.postgres;

import org.nmcpye.datarun.domain.Project;
import org.nmcpye.datarun.drun.postgres.repository.ProjectRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.ProjectServiceCustom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing {@link Project}.
 */
@RestController
@RequestMapping("/api/custom/projects")
public class ProjectResourceCustom extends AbstractRelationalResource<Project> {

    private final Logger log = LoggerFactory.getLogger(ProjectResourceCustom.class);

    private final ProjectServiceCustom projectService;

    private final ProjectRelationalRepositoryCustom projectRepository;

    public ProjectResourceCustom(ProjectServiceCustom projectService,
                                 ProjectRelationalRepositoryCustom projectRepository) {
        super(projectService, projectRepository);
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
