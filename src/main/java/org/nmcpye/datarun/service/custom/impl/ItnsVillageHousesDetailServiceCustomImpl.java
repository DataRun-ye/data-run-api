package org.nmcpye.datarun.service.custom.impl;

import org.nmcpye.datarun.domain.ItnsVillageHousesDetail;
import org.nmcpye.datarun.repository.ItnsVillageHousesDetailRepository;
import org.nmcpye.datarun.repository.ItnsVillageHousesDetailRepositoryCustom;
import org.nmcpye.datarun.service.custom.ItnsVillageHousesDetailServiceCustom;
import org.nmcpye.datarun.service.impl.ItnsVillageHousesDetailServiceImpl;
import org.nmcpye.datarun.utils.CodeGenerator;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Primary
@Transactional
public class ItnsVillageHousesDetailServiceCustomImpl
    extends ItnsVillageHousesDetailServiceImpl implements ItnsVillageHousesDetailServiceCustom {
    ItnsVillageHousesDetailRepositoryCustom itnsVillageHousesDetailRepositoryCustom;

    public ItnsVillageHousesDetailServiceCustomImpl(
        ItnsVillageHousesDetailRepository itnsVillageHousesDetailRepository,
        ItnsVillageHousesDetailRepositoryCustom itnsVillageHousesDetailRepositoryCustom) {
        super(itnsVillageHousesDetailRepository);
        this.itnsVillageHousesDetailRepositoryCustom = itnsVillageHousesDetailRepositoryCustom;
    }

    @Override
    public ItnsVillageHousesDetail save(ItnsVillageHousesDetail itnsVillageHousesDetail) {
        if (itnsVillageHousesDetail.getUid() == null || itnsVillageHousesDetail.getUid().isEmpty()) {
            itnsVillageHousesDetail.setUid(CodeGenerator.generateUid());
        }
        return super.save(itnsVillageHousesDetail);
    }
}
