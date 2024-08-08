package org.nmcpye.datarun.service.impl;

import java.util.Optional;
import org.nmcpye.datarun.domain.OrganizationUnit;
import org.nmcpye.datarun.repository.OrganizationUnitRepository;
import org.nmcpye.datarun.service.OrganizationUnitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link org.nmcpye.datarun.domain.OrganizationUnit}.
 */
@Service
@Transactional
public class OrganizationUnitServiceImpl implements OrganizationUnitService {

    private static final Logger log = LoggerFactory.getLogger(OrganizationUnitServiceImpl.class);

    private final OrganizationUnitRepository organizationUnitRepository;

    public OrganizationUnitServiceImpl(OrganizationUnitRepository organizationUnitRepository) {
        this.organizationUnitRepository = organizationUnitRepository;
    }

    @Override
    public OrganizationUnit save(OrganizationUnit organizationUnit) {
        log.debug("Request to save OrganizationUnit : {}", organizationUnit);
        return organizationUnitRepository.save(organizationUnit);
    }

    @Override
    public OrganizationUnit update(OrganizationUnit organizationUnit) {
        log.debug("Request to update OrganizationUnit : {}", organizationUnit);
        return organizationUnitRepository.save(organizationUnit);
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
                if (organizationUnit.getName() != null) {
                    existingOrganizationUnit.setName(organizationUnit.getName());
                }
                if (organizationUnit.getOuPath() != null) {
                    existingOrganizationUnit.setOuPath(organizationUnit.getOuPath());
                }
                if (organizationUnit.getParent() != null) {
                    existingOrganizationUnit.setParent(organizationUnit.getParent());
                }
                if (organizationUnit.getParentCode() != null) {
                    existingOrganizationUnit.setParentCode(organizationUnit.getParentCode());
                }

                return existingOrganizationUnit;
            })
            .map(organizationUnitRepository::save);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrganizationUnit> findAll(Pageable pageable) {
        log.debug("Request to get all OrganizationUnits");
        return organizationUnitRepository.findAll(pageable);
    }

    public Page<OrganizationUnit> findAllWithEagerRelationships(Pageable pageable) {
        return organizationUnitRepository.findAllWithEagerRelationships(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrganizationUnit> findOne(Long id) {
        log.debug("Request to get OrganizationUnit : {}", id);
        return organizationUnitRepository.findOneWithEagerRelationships(id);
    }

    @Override
    public void delete(Long id) {
        log.debug("Request to delete OrganizationUnit : {}", id);
        organizationUnitRepository.deleteById(id);
    }
}
