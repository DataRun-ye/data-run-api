package org.nmcpye.datarun.drun.postgres.service.impl;

import org.nmcpye.datarun.domain.ItnsVillage;
import org.nmcpye.datarun.drun.postgres.repository.ItnsVillageRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.IdentifiableServiceImpl;
import org.nmcpye.datarun.drun.postgres.service.ItnsVillageServiceCustom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Primary
@Transactional
public class ItnsVillageServiceCustomImpl
    extends IdentifiableServiceImpl<ItnsVillage>
    implements ItnsVillageServiceCustom {

    private final Logger log = LoggerFactory.getLogger(ItnsVillageServiceCustomImpl.class);

    final private ItnsVillageRepositoryCustom itnsVillageRepository;

    public ItnsVillageServiceCustomImpl(ItnsVillageRepositoryCustom itnsVillageRepository) {
        super(itnsVillageRepository);
        this.itnsVillageRepository = itnsVillageRepository;
    }

    @Override
    public Optional<ItnsVillage> partialUpdate(ItnsVillage itnsVillage) {
        log.debug("Request to partially update ItnsVillage : {}", itnsVillage);

        return itnsVillageRepository
            .findById(itnsVillage.getId())
            .map(existingItnsVillage -> {
                if (itnsVillage.getUid() != null) {
                    existingItnsVillage.setUid(itnsVillage.getUid());
                }
                if (itnsVillage.getCode() != null) {
                    existingItnsVillage.setCode(itnsVillage.getCode());
                }
                if (itnsVillage.getName() != null) {
                    existingItnsVillage.setName(itnsVillage.getName());
                }
                if (itnsVillage.getSubmissionUuid() != null) {
                    existingItnsVillage.setSubmissionUuid(itnsVillage.getSubmissionUuid());
                }
                if (itnsVillage.getSubmissionId() != null) {
                    existingItnsVillage.setSubmissionId(itnsVillage.getSubmissionId());
                }
                if (itnsVillage.getWorkDayDate() != null) {
                    existingItnsVillage.setWorkDayDate(itnsVillage.getWorkDayDate());
                }
                if (itnsVillage.getSurveytype() != null) {
                    existingItnsVillage.setSurveytype(itnsVillage.getSurveytype());
                }
                if (itnsVillage.getOtherReasonComment() != null) {
                    existingItnsVillage.setOtherReasonComment(itnsVillage.getOtherReasonComment());
                }
                if (itnsVillage.getReasonNotcomplete() != null) {
                    existingItnsVillage.setReasonNotcomplete(itnsVillage.getReasonNotcomplete());
                }
                if (itnsVillage.getSettlement() != null) {
                    existingItnsVillage.setSettlement(itnsVillage.getSettlement());
                }
                if (itnsVillage.getSettlementName() != null) {
                    existingItnsVillage.setSettlementName(itnsVillage.getSettlementName());
                }
                if (itnsVillage.getTlCommenet() != null) {
                    existingItnsVillage.setTlCommenet(itnsVillage.getTlCommenet());
                }
                if (itnsVillage.getTimeSpentHours() != null) {
                    existingItnsVillage.setTimeSpentHours(itnsVillage.getTimeSpentHours());
                }
                if (itnsVillage.getTimeSpentMinutes() != null) {
                    existingItnsVillage.setTimeSpentMinutes(itnsVillage.getTimeSpentMinutes());
                }
                if (itnsVillage.getDifficulties() != null) {
                    existingItnsVillage.setDifficulties(itnsVillage.getDifficulties());
                }
                if (itnsVillage.getLocationCaptured() != null) {
                    existingItnsVillage.setLocationCaptured(itnsVillage.getLocationCaptured());
                }
                if (itnsVillage.getLocationCaptureTime() != null) {
                    existingItnsVillage.setLocationCaptureTime(itnsVillage.getLocationCaptureTime());
                }
                if (itnsVillage.getHoProof() != null) {
                    existingItnsVillage.setHoProof(itnsVillage.getHoProof());
                }
                if (itnsVillage.getHoProofUrl() != null) {
                    existingItnsVillage.setHoProofUrl(itnsVillage.getHoProofUrl());
                }
                if (itnsVillage.getSubmissionTime() != null) {
                    existingItnsVillage.setSubmissionTime(itnsVillage.getSubmissionTime());
                }
                if (itnsVillage.getUntargetingOtherSpecify() != null) {
                    existingItnsVillage.setUntargetingOtherSpecify(itnsVillage.getUntargetingOtherSpecify());
                }
                if (itnsVillage.getOtherVillageName() != null) {
                    existingItnsVillage.setOtherVillageName(itnsVillage.getOtherVillageName());
                }
                if (itnsVillage.getOtherVillageCode() != null) {
                    existingItnsVillage.setOtherVillageCode(itnsVillage.getOtherVillageCode());
                }
                if (itnsVillage.getOtherTeamNo() != null) {
                    existingItnsVillage.setOtherTeamNo(itnsVillage.getOtherTeamNo());
                }
                if (itnsVillage.getStartEntryTime() != null) {
                    existingItnsVillage.setStartEntryTime(itnsVillage.getStartEntryTime());
                }
                if (itnsVillage.getFinishedEntryTime() != null) {
                    existingItnsVillage.setFinishedEntryTime(itnsVillage.getFinishedEntryTime());
                }
                if (itnsVillage.getDeleted() != null) {
                    existingItnsVillage.setDeleted(itnsVillage.getDeleted());
                }
                if (itnsVillage.getStatus() != null) {
                    existingItnsVillage.setStatus(itnsVillage.getStatus());
                }
                if (itnsVillage.getCreatedBy() != null) {
                    existingItnsVillage.setCreatedBy(itnsVillage.getCreatedBy());
                }
                if (itnsVillage.getCreatedDate() != null) {
                    existingItnsVillage.setCreatedDate(itnsVillage.getCreatedDate());
                }
                if (itnsVillage.getLastModifiedBy() != null) {
                    existingItnsVillage.setLastModifiedBy(itnsVillage.getLastModifiedBy());
                }
                if (itnsVillage.getLastModifiedDate() != null) {
                    existingItnsVillage.setLastModifiedDate(itnsVillage.getLastModifiedDate());
                }

                return existingItnsVillage;
            })
            .map(itnsVillageRepository::save);
    }

}
