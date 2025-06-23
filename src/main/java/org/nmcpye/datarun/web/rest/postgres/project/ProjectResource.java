package org.nmcpye.datarun.web.rest.postgres.project;

import org.nmcpye.datarun.domain.Project;
import org.nmcpye.datarun.drun.postgres.repository.ProjectRepository;
import org.nmcpye.datarun.drun.postgres.service.ProjectService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.web.rest.common.ApiVersion;
import org.nmcpye.datarun.web.rest.postgres.JpaBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.nmcpye.datarun.web.rest.postgres.project.ProjectResource.CUSTOM;
import static org.nmcpye.datarun.web.rest.postgres.project.ProjectResource.V1;

/**
 * REST controller for managing {@link Project}.
 */
@RestController
@RequestMapping(value = {CUSTOM, V1})
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
public class ProjectResource extends JpaBaseResource<Project> {
    protected static final String NAME = "/projects";
    protected static final String CUSTOM = ApiVersion.API_CUSTOM + NAME;
    protected static final String V1 = ApiVersion.API_V1 + NAME;

    private final Logger log = LoggerFactory.getLogger(ProjectResource.class);

    private final ProjectService projectService;

    private final ProjectRepository projectRepository;

    public ProjectResource(ProjectService projectService,
                           ProjectRepository projectRepository) {
        super(projectService, projectRepository);
        this.projectService = projectService;
        this.projectRepository = projectRepository;
    }

    @Override
    protected String getName() {
        return "projects";
    }

//    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
//    @Override
//    public ResponseEntity<Project> updateEntity(String uid, Project entity) throws URISyntaxException {
//        return super.updateEntity(uid, entity);
//    }
//
//    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
//    @Override
//    public ResponseEntity<?> saveReturnSaved(Project entity) {
//        return super.saveReturnSaved(entity);
//    }
//
//    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
//    @Override
//    public ResponseEntity<EntitySaveSummaryVM> saveOne(Project entity) {
//        return super.saveOne(entity);
//    }
//
//    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
//    @Override
//    public ResponseEntity<EntitySaveSummaryVM> saveAll(List<Project> entities) {
//        return super.saveAll(entities);
//    }
}
