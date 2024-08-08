package org.nmcpye.datarun.service.impl;

import java.util.Optional;
import org.nmcpye.datarun.domain.VillageLocation;
import org.nmcpye.datarun.repository.VillageLocationRepository;
import org.nmcpye.datarun.service.VillageLocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link org.nmcpye.datarun.domain.VillageLocation}.
 */
@Service
@Transactional
public class VillageLocationServiceImpl implements VillageLocationService {

    private static final Logger log = LoggerFactory.getLogger(VillageLocationServiceImpl.class);

    private final VillageLocationRepository villageLocationRepository;

    public VillageLocationServiceImpl(VillageLocationRepository villageLocationRepository) {
        this.villageLocationRepository = villageLocationRepository;
    }

    @Override
    public VillageLocation save(VillageLocation villageLocation) {
        log.debug("Request to save VillageLocation : {}", villageLocation);
        return villageLocationRepository.save(villageLocation);
    }

    @Override
    public VillageLocation update(VillageLocation villageLocation) {
        log.debug("Request to update VillageLocation : {}", villageLocation);
        villageLocation.setIsPersisted();
        return villageLocationRepository.save(villageLocation);
    }

    @Override
    public Optional<VillageLocation> partialUpdate(VillageLocation villageLocation) {
        log.debug("Request to partially update VillageLocation : {}", villageLocation);

        return villageLocationRepository
            .findById(villageLocation.getId())
            .map(existingVillageLocation -> {
                if (villageLocation.getUid() != null) {
                    existingVillageLocation.setUid(villageLocation.getUid());
                }
                if (villageLocation.getCode() != null) {
                    existingVillageLocation.setCode(villageLocation.getCode());
                }
                if (villageLocation.getName() != null) {
                    existingVillageLocation.setName(villageLocation.getName());
                }
                if (villageLocation.getMappingStatus() != null) {
                    existingVillageLocation.setMappingStatus(villageLocation.getMappingStatus());
                }
                if (villageLocation.getDistrictCode() != null) {
                    existingVillageLocation.setDistrictCode(villageLocation.getDistrictCode());
                }
                if (villageLocation.getVillageUid() != null) {
                    existingVillageLocation.setVillageUid(villageLocation.getVillageUid());
                }
                if (villageLocation.getSubdistrictName() != null) {
                    existingVillageLocation.setSubdistrictName(villageLocation.getSubdistrictName());
                }
                if (villageLocation.getVillageName() != null) {
                    existingVillageLocation.setVillageName(villageLocation.getVillageName());
                }
                if (villageLocation.getSubvillageName() != null) {
                    existingVillageLocation.setSubvillageName(villageLocation.getSubvillageName());
                }
                if (villageLocation.getUrbanRuralId() != null) {
                    existingVillageLocation.setUrbanRuralId(villageLocation.getUrbanRuralId());
                }
                if (villageLocation.getUrbanRural() != null) {
                    existingVillageLocation.setUrbanRural(villageLocation.getUrbanRural());
                }
                if (villageLocation.getSettlement() != null) {
                    existingVillageLocation.setSettlement(villageLocation.getSettlement());
                }
                if (villageLocation.getPop2004() != null) {
                    existingVillageLocation.setPop2004(villageLocation.getPop2004());
                }
                if (villageLocation.getPop2022() != null) {
                    existingVillageLocation.setPop2022(villageLocation.getPop2022());
                }
                if (villageLocation.getLongitude() != null) {
                    existingVillageLocation.setLongitude(villageLocation.getLongitude());
                }
                if (villageLocation.getLatitude() != null) {
                    existingVillageLocation.setLatitude(villageLocation.getLatitude());
                }
                if (villageLocation.getPpcCodeGis() != null) {
                    existingVillageLocation.setPpcCodeGis(villageLocation.getPpcCodeGis());
                }
                if (villageLocation.getLevel() != null) {
                    existingVillageLocation.setLevel(villageLocation.getLevel());
                }
                if (villageLocation.getCreatedBy() != null) {
                    existingVillageLocation.setCreatedBy(villageLocation.getCreatedBy());
                }
                if (villageLocation.getCreatedDate() != null) {
                    existingVillageLocation.setCreatedDate(villageLocation.getCreatedDate());
                }
                if (villageLocation.getLastModifiedBy() != null) {
                    existingVillageLocation.setLastModifiedBy(villageLocation.getLastModifiedBy());
                }
                if (villageLocation.getLastModifiedDate() != null) {
                    existingVillageLocation.setLastModifiedDate(villageLocation.getLastModifiedDate());
                }

                return existingVillageLocation;
            })
            .map(villageLocationRepository::save);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VillageLocation> findAll(Pageable pageable) {
        log.debug("Request to get all VillageLocations");
        return villageLocationRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VillageLocation> findOne(Long id) {
        log.debug("Request to get VillageLocation : {}", id);
        return villageLocationRepository.findById(id);
    }

    @Override
    public void delete(Long id) {
        log.debug("Request to delete VillageLocation : {}", id);
        villageLocationRepository.deleteById(id);
    }
}
