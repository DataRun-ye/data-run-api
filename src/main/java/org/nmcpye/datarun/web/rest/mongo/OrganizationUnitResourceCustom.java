package org.nmcpye.datarun.web.rest.mongo;

import org.nmcpye.datarun.drun.mongo.domain.OrganizationUnit;
import org.nmcpye.datarun.drun.mongo.repository.OrganizationUnitRepositoryCustom;
import org.nmcpye.datarun.drun.mongo.service.OrganizationUnitServiceCustom;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing {@link OrganizationUnit}.
 */
@RestController
@RequestMapping("/api/custom/orgUnits")
public class OrganizationUnitResourceCustom extends AbstractMongoResource<OrganizationUnit> {

    public OrganizationUnitResourceCustom(OrganizationUnitServiceCustom organizationUnitServiceCustom,
                                          OrganizationUnitRepositoryCustom organizationUnitRepositoryCustom) {
        super(organizationUnitServiceCustom, organizationUnitRepositoryCustom);
    }

    @Override
    protected String getName() {
        return "orgUnits";
    }
}
