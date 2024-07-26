package org.nmcpye.datarun.web.rest;

import org.nmcpye.datarun.domain.ChvSupply;
import org.nmcpye.datarun.drun.postgres.repository.ChvSupplyRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.ChvSupplyServiceCustom;
import org.nmcpye.datarun.web.rest.common.AbstractResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing {@link ChvSupply}.
 */
@RestController
@RequestMapping("/api/custom/chvSupplies")
public class ChvSupplyResourceCustom extends AbstractResource<ChvSupply> {

    private final Logger log = LoggerFactory.getLogger(ChvSupplyResourceCustom.class);

    private final ChvSupplyServiceCustom chvSupplyService;

    private final ChvSupplyRepositoryCustom chvSupplyRepository;

    public ChvSupplyResourceCustom(ChvSupplyServiceCustom chvSupplyService,
                                   ChvSupplyRepositoryCustom chvSupplyRepository) {
        super(chvSupplyService, chvSupplyRepository);
        this.chvSupplyService = chvSupplyService;
        this.chvSupplyRepository = chvSupplyRepository;
    }

    @Override
    protected Page<ChvSupply> getList(Pageable pageable, boolean eagerload) {
        if (eagerload) {
            return chvSupplyService.findAllWithEagerRelationships(pageable);
        } else {
            return chvSupplyService.findAll(pageable);
        }
    }

    @Override
    protected String getName() {
        return "chvSupplies";
    }
}
