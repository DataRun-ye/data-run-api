package org.nmcpye.datarun.web.rest;

import org.nmcpye.datarun.domain.ChvSession;
import org.nmcpye.datarun.drun.postgres.repository.ChvSessionRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.ChvSessionServiceCustom;
import org.nmcpye.datarun.web.rest.common.AbstractResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Custom controller for managing {@link ChvSession}.
 */
@RestController
@RequestMapping("/api/custom/chvSessions")
public class ChvSessionResourceCustom extends AbstractResource<ChvSession> {

    private final Logger log = LoggerFactory.getLogger(ChvSessionResourceCustom.class);

    private final ChvSessionServiceCustom chvSessionService;

    private final ChvSessionRepositoryCustom chvSessionRepository;

    public ChvSessionResourceCustom(ChvSessionServiceCustom chvSessionService,
                                    ChvSessionRepositoryCustom chvSessionRepository) {
        super(chvSessionService, chvSessionRepository);
        this.chvSessionService = chvSessionService;
        this.chvSessionRepository = chvSessionRepository;
    }

    @Override
    protected Page<ChvSession> getList(Pageable pageable, boolean eagerload) {
        if (eagerload) {
            return chvSessionService.findAllWithEagerRelationships(pageable);
        } else {
            return chvSessionService.findAll(pageable);
        }
    }

    @Override
    protected String getName() {
        return "chvSessions";
    }
}
