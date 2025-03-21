package org.nmcpye.datarun.web.rest.postgres.project;

import org.nmcpye.datarun.domain.Project;
import org.nmcpye.datarun.drun.postgres.repository.ProjectRepository;
import org.nmcpye.datarun.drun.postgres.service.ProjectService;
import org.nmcpye.datarun.mongo.mapping.importsummary.EntitySaveSummaryVM;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.web.rest.postgres.AbstractJpaResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URISyntaxException;
import java.util.List;

/**
 * REST controller for managing {@link Project}.
 */
@RestController
@RequestMapping("/api/custom/projects")
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
public class ProjectResourceCustom extends AbstractJpaResource<Project> {

    private final Logger log = LoggerFactory.getLogger(ProjectResourceCustom.class);

    private final ProjectService projectService;

    private final ProjectRepository projectRepository;

    public ProjectResourceCustom(ProjectService projectService,
                                 ProjectRepository projectRepository) {
        super(projectService, projectRepository);
        this.projectService = projectService;
        this.projectRepository = projectRepository;
    }

    @Override
    protected String getName() {
        return "projects";
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<Project> updateEntity(Long aLong, Project entity) throws URISyntaxException {
        return super.updateEntity(aLong, entity);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<?> saveReturnSaved(Project entity) {
        return super.saveReturnSaved(entity);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<EntitySaveSummaryVM> saveOne(Project entity) {
        return super.saveOne(entity);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<EntitySaveSummaryVM> saveAll(List<Project> entities) {
        return super.saveAll(entities);
    }
}
