package org.nmcpye.datarun.web.rest.postgres;

import org.nmcpye.datarun.domain.OrgUnit;
import org.nmcpye.datarun.drun.postgres.repository.OrgUnitRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.OrgUnitServiceCustom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Extended controller for managing {@link OrgUnit}.
 */
@RestController
@RequestMapping("/api/custom/orgUnits")
public class OrgUnitResourceCustom extends AbstractRelationalResource<OrgUnit> {

    private final Logger log = LoggerFactory.getLogger(OrgUnitResourceCustom.class);

    private final OrgUnitServiceCustom orgUnitService;

    private final OrgUnitRelationalRepositoryCustom orgUnitRepository;

    public OrgUnitResourceCustom(OrgUnitServiceCustom orgUnitService, OrgUnitRelationalRepositoryCustom orgUnitRepository) {
        super(orgUnitService, orgUnitRepository);
        this.orgUnitRepository = orgUnitRepository;
        this.orgUnitService = orgUnitService;
    }

    @Override
    protected Page<OrgUnit> getList(Pageable pageable, boolean eagerload) {
        return orgUnitService.findAllByUser(pageable);

    }

    @Override
    protected String getName() {
        return "orgUnits";
    }
}
