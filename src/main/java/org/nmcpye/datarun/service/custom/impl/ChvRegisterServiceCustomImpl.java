package org.nmcpye.datarun.service.custom.impl;

import org.nmcpye.datarun.domain.ChvRegister;
import org.nmcpye.datarun.repository.ChvRegisterRepositoryCustom;
import org.nmcpye.datarun.service.custom.ChvRegisterServiceCustom;
import org.nmcpye.datarun.service.impl.ChvRegisterServiceImpl;
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
public class ChvRegisterServiceCustomImpl
    extends ChvRegisterServiceImpl
    implements ChvRegisterServiceCustom {

    private final Logger log = LoggerFactory.getLogger(ChvRegisterServiceImpl.class);

    private final ChvRegisterRepositoryCustom chvRegisterRepository;


    public ChvRegisterServiceCustomImpl(ChvRegisterRepositoryCustom chvRegisterRepository) {
        super(chvRegisterRepository);
        this.chvRegisterRepository = chvRegisterRepository;
    }

    @Override
    public ChvRegister save(ChvRegister chvRegister) {
        if (chvRegister.getUid() == null || chvRegister.getUid().isEmpty()) {
            chvRegister.setUid(CodeGenerator.generateUid());
        }
        log.debug("Request to save ChvRegister : {}", chvRegister);
        return chvRegisterRepository.save(chvRegister);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ChvRegister> findAll(Pageable pageable) {
        log.debug("Request to get all ChvRegisters");
        return chvRegisterRepository.findAllByUser(pageable);
    }

    public Page<ChvRegister> findAllWithEagerRelationships(Pageable pageable) {
        return chvRegisterRepository.findAllWithEagerRelationshipsByUser(pageable);
    }

}
