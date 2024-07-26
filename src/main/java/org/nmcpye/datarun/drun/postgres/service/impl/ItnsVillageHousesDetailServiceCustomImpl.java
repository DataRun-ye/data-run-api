package org.nmcpye.datarun.drun.postgres.service.impl;

import org.nmcpye.datarun.domain.ItnsVillageHousesDetail;
import org.nmcpye.datarun.drun.postgres.repository.ItnsVillageHousesDetailRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.IdentifiableService;
import org.nmcpye.datarun.drun.postgres.service.ItnsVillageHousesDetailServiceCustom;
import org.nmcpye.datarun.service.impl.ItnsVillageHousesDetailServiceImpl;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Primary
@Transactional
public class ItnsVillageHousesDetailServiceCustomImpl
    extends ItnsVillageHousesDetailServiceImpl
    implements ItnsVillageHousesDetailServiceCustom,
    IdentifiableService<ItnsVillageHousesDetail> {
    ItnsVillageHousesDetailRepositoryCustom itnsVillageHousesDetailRepository;

    public ItnsVillageHousesDetailServiceCustomImpl(
        ItnsVillageHousesDetailRepositoryCustom itnsVillageHousesDetailRepositoryCustom) {
        super(itnsVillageHousesDetailRepositoryCustom);
        this.itnsVillageHousesDetailRepository = itnsVillageHousesDetailRepositoryCustom;
    }

    @Override
    public boolean existsByUid(String uid) {
        return itnsVillageHousesDetailRepository.findByUid(uid).isPresent();
    }

    @Override
    public Optional<ItnsVillageHousesDetail> findByUid(String uid) {
        return itnsVillageHousesDetailRepository.findByUid(uid);
    }

    @Override
    public void deleteByUid(String uid) {
        itnsVillageHousesDetailRepository.deleteByUid(uid);
    }

    @Override
    public Page<ItnsVillageHousesDetail> findAllWithEagerRelationships(Pageable pageable) {
        return itnsVillageHousesDetailRepository.findAll(pageable);
    }
}
