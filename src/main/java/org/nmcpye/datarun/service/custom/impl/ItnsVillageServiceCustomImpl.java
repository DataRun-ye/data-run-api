package org.nmcpye.datarun.service.custom.impl;

import org.nmcpye.datarun.domain.ItnsVillage;
import org.nmcpye.datarun.repository.ItnsVillageRepositoryCustom;
import org.nmcpye.datarun.service.custom.ItnsVillageServiceCustom;
import org.nmcpye.datarun.service.impl.ItnsVillageServiceImpl;
import org.nmcpye.datarun.utils.CodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Primary
@Transactional
public class ItnsVillageServiceCustomImpl extends ItnsVillageServiceImpl implements ItnsVillageServiceCustom {

    private final Logger log = LoggerFactory.getLogger(ItnsVillageServiceCustomImpl.class);

    final private ItnsVillageRepositoryCustom itnsVillageRepository;

    public ItnsVillageServiceCustomImpl(ItnsVillageRepositoryCustom itnsVillageRepository) {
        super(itnsVillageRepository);
        this.itnsVillageRepository = itnsVillageRepository;
    }

    @Override
    public ItnsVillage save(ItnsVillage itnsVillage) {
        if (itnsVillage.getUid() == null || itnsVillage.getUid().isEmpty()) {
            itnsVillage.setUid(CodeGenerator.generateUid());
        }
        return itnsVillageRepository.save(itnsVillage);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ItnsVillage> findAll(Pageable pageable) {
        log.debug("Request to get all ItnsVillages");
        return itnsVillageRepository.findAllByUser(pageable);
    }

    @Override
    public Page<ItnsVillage> findAllWithEagerRelationships(Pageable pageable) {
        return itnsVillageRepository.findAllWithEagerRelationshipsByUser(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ItnsVillage> findOne(Long id) {
        log.debug("Request to get ItnsVillage : {}", id);
        return itnsVillageRepository.findOneWithEagerRelationshipsByUser(id);
    }
}
