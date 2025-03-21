//package org.nmcpye.datarun.service.impl;
//
//import org.nmcpye.datarun.drun.postgres.domain.OrgUnit;
//import org.nmcpye.datarun.repository.OrgUnitRepository;
//import org.nmcpye.datarun.service.OrgUnitService;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.Optional;
//
///**
// * Service Implementation for managing {@link OrgUnit}.
// */
//@Service
//@Transactional
//public class OrgUnitServiceImpl implements OrgUnitService {
//
//    private static final Logger log = LoggerFactory.getLogger(OrgUnitServiceImpl.class);
//
//    private final OrgUnitRepository orgUnitRepository;
//
//    public OrgUnitServiceImpl(OrgUnitRepository orgUnitRepository) {
//        this.orgUnitRepository = orgUnitRepository;
//    }
//
//    @Override
//    public OrgUnit save(OrgUnit orgUnit) {
//        log.debug("Request to save OrgUnit : {}", orgUnit);
//        return orgUnitRepository.save(orgUnit);
//    }
//
//    @Override
//    public OrgUnit update(OrgUnit orgUnit) {
//        log.debug("Request to update OrgUnit : {}", orgUnit);
//        return orgUnitRepository.save(orgUnit);
//    }
//
//    @Override
//    public Optional<OrgUnit> partialUpdate(OrgUnit orgUnit) {
//        log.debug("Request to partially update OrgUnit : {}", orgUnit);
//
//        return orgUnitRepository
//            .findById(orgUnit.getId())
//            .map(existingOrgUnit -> {
//                if (orgUnit.getUid() != null) {
//                    existingOrgUnit.setUid(orgUnit.getUid());
//                }
//                if (orgUnit.getCode() != null) {
//                    existingOrgUnit.setCode(orgUnit.getCode());
//                }
//                if (orgUnit.getName() != null) {
//                    existingOrgUnit.setName(orgUnit.getName());
//                }
//                if (orgUnit.getPath() != null) {
//                    existingOrgUnit.setPath(orgUnit.getPath());
//                }
//
//                return existingOrgUnit;
//            })
//            .map(orgUnitRepository::save);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public Page<OrgUnit> findAll(Pageable pageable) {
//        log.debug("Request to get all OrgUnits");
//        return orgUnitRepository.findAll(pageable);
//    }
//
//    public Page<OrgUnit> findAllWithEagerRelationships(Pageable pageable) {
//        return orgUnitRepository.findAllWithEagerRelationships(pageable);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public Optional<OrgUnit> findOne(Long id) {
//        log.debug("Request to get OrgUnit : {}", id);
//        return orgUnitRepository.findOneWithEagerRelationships(id);
//    }
//
//    @Override
//    public void delete(Long id) {
//        log.debug("Request to delete OrgUnit : {}", id);
//        orgUnitRepository.deleteById(id);
//    }
//}
