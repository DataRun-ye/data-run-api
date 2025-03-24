package org.nmcpye.datarun.web.rest.postgres.orgunit;

import org.nmcpye.datarun.domain.Project;
import org.nmcpye.datarun.drun.postgres.domain.OuLevel;
import org.nmcpye.datarun.drun.postgres.repository.OuLevelRepository;
import org.nmcpye.datarun.drun.postgres.service.OuLevelService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.web.rest.postgres.JpaBaseResource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing {@link Project}.
 */
@RestController
@RequestMapping("/api/custom/ouLevels")
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
public class OuLevelResourceCustom
    extends JpaBaseResource<OuLevel> {

    private final OuLevelService ouLevelService;

    private final OuLevelRepository ouLevelRepositoryCustom;

    public OuLevelResourceCustom(OuLevelService ouLevelService,
                                 OuLevelRepository ouLevelRepository) {
        super(ouLevelService, ouLevelRepository);
        this.ouLevelService = ouLevelService;
        this.ouLevelRepositoryCustom = ouLevelRepository;
    }

    @Override
    protected String getName() {
        return "ouLevels";
    }
}
