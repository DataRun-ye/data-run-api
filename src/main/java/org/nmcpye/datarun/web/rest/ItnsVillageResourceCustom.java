package org.nmcpye.datarun.web.rest;

import org.nmcpye.datarun.domain.ItnsVillage;
import org.nmcpye.datarun.drun.postgres.repository.ItnsVillageRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.ItnsVillageServiceCustom;
import org.nmcpye.datarun.web.rest.common.AbstractResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.jhipster.web.util.ResponseUtil;

import java.util.Optional;

/**
 * REST controller for managing {@link ItnsVillage}.
 */
@RestController
@RequestMapping("/api/custom/itnsVillages")
public class ItnsVillageResourceCustom
    extends AbstractResource<ItnsVillage> {

    private final Logger log = LoggerFactory.getLogger(ItnsVillageResourceCustom.class);

    private final ItnsVillageServiceCustom itnsVillageService;

    private final ItnsVillageRepositoryCustom itnsVillageRepository;

    public ItnsVillageResourceCustom(ItnsVillageServiceCustom itnsVillageService,
                                     ItnsVillageRepositoryCustom itnsVillageRepository) {
        super(itnsVillageService, itnsVillageRepository);
        this.itnsVillageService = itnsVillageService;
        this.itnsVillageRepository = itnsVillageRepository;
    }

    @Override
    protected Page<ItnsVillage> getList(Pageable pageable, boolean eagerload) {
        if (eagerload) {
            return itnsVillageService.findAllWithEagerRelationships(pageable);
        } else {
            return itnsVillageService.findAll(pageable);
        }
    }

    @Override
    protected String getName() {
        return "itnsVillages";
    }

    /**
     * {@code GET  /itns-villages/:id} : get the "id" itnsVillage.
     *
     * @param id the id of the itnsVillage to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the itnsVillage, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ItnsVillage> getOneByUser(@PathVariable("id") Long id) {
        log.debug("REST request to get ItnsVillage : {}", id);
        Optional<ItnsVillage> itnsVillage = itnsVillageService.findOne(id);
        return ResponseUtil.wrapOrNotFound(itnsVillage);
    }
}
