package org.nmcpye.datarun.service.custom.impl;

import org.nmcpye.datarun.domain.ChvSession;
import org.nmcpye.datarun.repository.ChvSessionRepositoryCustom;
import org.nmcpye.datarun.service.custom.ChvSessionServiceCustom;
import org.nmcpye.datarun.service.impl.ChvSessionServiceImpl;
import org.nmcpye.datarun.utils.CodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Primary
@Transactional
public class ChvSessionServiceCustomImpl
    extends ChvSessionServiceImpl
    implements ChvSessionServiceCustom {

    private final Logger log = LoggerFactory.getLogger(ChvSessionServiceCustomImpl.class);

    private final ChvSessionRepositoryCustom chvSessionRepository;


    public ChvSessionServiceCustomImpl(ChvSessionRepositoryCustom chvSessionRepository) {
        super(chvSessionRepository);
        this.chvSessionRepository = chvSessionRepository;
    }

    @Override
    public ChvSession save(ChvSession chvSession) {
        if (chvSession.getUid() == null || chvSession.getUid().isEmpty()) {
            chvSession.setUid(CodeGenerator.generateUid());
        }
        return chvSessionRepository.save(chvSession);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ChvSession> findAll(Pageable pageable) {
        log.debug("Request to get all ChvSession");
        return chvSessionRepository.findAllByUser(pageable);
    }

    public Page<ChvSession> findAllWithEagerRelationships(Pageable pageable) {
        return chvSessionRepository.findAllWithEagerRelationshipsByUser(pageable);
    }
}
