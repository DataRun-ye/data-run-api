package org.nmcpye.datarun.web.rest.postgres;

import org.nmcpye.datarun.domain.Project;
import org.nmcpye.datarun.drun.postgres.domain.OuLevel;
import org.nmcpye.datarun.drun.postgres.repository.OuLevelRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.OuLevelServiceCustom;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing {@link Project}.
 */
@RestController
@RequestMapping("/api/custom/ouLevels")
public class OuLevelResourceCustom
    extends AbstractRelationalResource<OuLevel> {

    private final OuLevelServiceCustom ouLevelServiceCustom;

    private final OuLevelRelationalRepositoryCustom ouLevelRepositoryCustom;

    public OuLevelResourceCustom(OuLevelServiceCustom ouLevelService,
                                 OuLevelRelationalRepositoryCustom ouLevelRepository) {
        super(ouLevelService, ouLevelRepository);
        this.ouLevelServiceCustom = ouLevelService;
        this.ouLevelRepositoryCustom = ouLevelRepository;
    }

    @Override
    protected String getName() {
        return "ouLevels";
    }
}
