package org.nmcpye.datarun.service.custom.impl;

import org.nmcpye.datarun.repository.PatientInfoRepositoryCustom;
import org.nmcpye.datarun.service.custom.PatientInfoServiceCustom;
import org.nmcpye.datarun.service.impl.PatientInfoServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Primary
@Transactional
public class PatientInfoServiceCustomImpl
    extends PatientInfoServiceImpl
    implements PatientInfoServiceCustom {

    private final Logger log = LoggerFactory.getLogger(PatientInfoServiceImpl.class);

    private final PatientInfoRepositoryCustom patientInfoRepository;

    public PatientInfoServiceCustomImpl(PatientInfoRepositoryCustom patientInfoRepository) {
        super(patientInfoRepository);
        this.patientInfoRepository = patientInfoRepository;
    }
}
