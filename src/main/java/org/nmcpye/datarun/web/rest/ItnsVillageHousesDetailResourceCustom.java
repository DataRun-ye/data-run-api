package org.nmcpye.datarun.web.rest;

import org.nmcpye.datarun.domain.ItnsVillageHousesDetail;
import org.nmcpye.datarun.drun.postgres.repository.ItnsVillageHousesDetailRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.ItnsVillageHousesDetailServiceCustom;
import org.nmcpye.datarun.web.rest.common.AbstractResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Custom controller for managing {@link ItnsVillageHousesDetail}.
 */
@RestController
@RequestMapping("/api/custom/itnsVillageHousesDetails")
public class ItnsVillageHousesDetailResourceCustom
    extends AbstractResource<ItnsVillageHousesDetail> {

    private final Logger log = LoggerFactory.getLogger(ItnsVillageHousesDetailResourceCustom.class);

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ItnsVillageHousesDetailServiceCustom itnsVillageHousesDetailService;

    private final ItnsVillageHousesDetailRepositoryCustom itnsVillageHousesDetailRepository;

    public ItnsVillageHousesDetailResourceCustom(
        ItnsVillageHousesDetailServiceCustom itnsVillageHousesDetailService,
        ItnsVillageHousesDetailRepositoryCustom itnsVillageHousesDetailRepository
    ) {
        super(itnsVillageHousesDetailService, itnsVillageHousesDetailRepository);
        this.itnsVillageHousesDetailService = itnsVillageHousesDetailService;
        this.itnsVillageHousesDetailRepository = itnsVillageHousesDetailRepository;
    }

    @Override
    protected Page<ItnsVillageHousesDetail> getList(Pageable pageable, boolean eagerload) {
        log.debug("REST request to get a page of ItnsVillageHousesDetails");
        return itnsVillageHousesDetailService.findAll(pageable);
    }

    @Override
    protected String getName() {
        return "itnsVillageHousesDetails";
    }
}
