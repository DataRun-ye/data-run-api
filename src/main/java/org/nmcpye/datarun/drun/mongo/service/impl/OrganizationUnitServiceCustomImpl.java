package org.nmcpye.datarun.drun.mongo.service.impl;

import org.nmcpye.datarun.drun.mongo.domain.OrganizationUnit;
import org.nmcpye.datarun.drun.mongo.repository.OrganizationUnitRepositoryCustom;
import org.nmcpye.datarun.drun.mongo.service.OrganizationUnitServiceCustom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service Implementation for managing {@link OrganizationUnit}.
 */
@Service
@Primary
public class OrganizationUnitServiceCustomImpl
    extends IdentifiableMongoServiceImpl<OrganizationUnit>
    implements OrganizationUnitServiceCustom {

    private final Logger log = LoggerFactory.getLogger(OrganizationUnitServiceCustomImpl.class);

    private final OrganizationUnitRepositoryCustom organizationUnitRepository;

    public OrganizationUnitServiceCustomImpl(
        OrganizationUnitRepositoryCustom organizationUnitRepository) {
        super(organizationUnitRepository);
        this.organizationUnitRepository = organizationUnitRepository;
    }

    @Override
    public Optional<OrganizationUnit> partialUpdate(OrganizationUnit organizationUnit) {
        log.debug("Request to partially update OrganizationUnit : {}", organizationUnit);

        return organizationUnitRepository
            .findById(organizationUnit.getId())
            .map(existingOrganizationUnit -> {
                if (organizationUnit.getUid() != null) {
                    existingOrganizationUnit.setUid(organizationUnit.getUid());
                }
                if (organizationUnit.getCode() != null) {
                    existingOrganizationUnit.setCode(organizationUnit.getCode());
                }

                if (organizationUnit.getDescription() != null) {
                    existingOrganizationUnit.setDescription(organizationUnit.getDescription());
                }
                if (organizationUnit.getDisabled() != null) {
                    existingOrganizationUnit.setDisabled(organizationUnit.getDisabled());
                }

                return existingOrganizationUnit;
            })
            .map(organizationUnitRepository::save);
    }
}
