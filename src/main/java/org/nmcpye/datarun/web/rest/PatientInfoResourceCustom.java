package org.nmcpye.datarun.web.rest;

import org.nmcpye.datarun.domain.PatientInfo;
import org.nmcpye.datarun.repository.PatientInfoRepositoryCustom;
import org.nmcpye.datarun.service.custom.PatientInfoServiceCustom;
import org.nmcpye.datarun.web.rest.common.AbstractResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

/**
 * REST Custom controller for managing {@link PatientInfo}.
 */
@RestController
@RequestMapping("/api/custom/patientInfo")
public class PatientInfoResourceCustom extends AbstractResource<PatientInfo> {

    private final Logger log = LoggerFactory.getLogger(PatientInfoResourceCustom.class);

    private final PatientInfoServiceCustom patientInfoService;

    private final PatientInfoRepositoryCustom patientInfoRepository;

    public PatientInfoResourceCustom(PatientInfoServiceCustom patientInfoService,
                                     PatientInfoRepositoryCustom patientInfoRepository) {
        this.patientInfoService = patientInfoService;
        this.patientInfoRepository = patientInfoRepository;
    }

    @Override
    protected Page<PatientInfo> getList(Pageable pageable, boolean eagerload) {
        if (eagerload) {
            return patientInfoService.findAllWithEagerRelationships(pageable);
        } else {
            return patientInfoService.findAll(pageable);
        }
    }

    @Override
    protected String getName() {
        return "patientInfo";
    }
}
