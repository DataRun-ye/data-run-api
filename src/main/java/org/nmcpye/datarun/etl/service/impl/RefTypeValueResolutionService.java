package org.nmcpye.datarun.etl.service.impl;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.etl.dto.RefTypeValue;
import org.nmcpye.datarun.etl.model.TallCanonicalRow;
import org.nmcpye.datarun.etl.repository.DimOptionJdbcRepository;
import org.nmcpye.datarun.etl.repository.DimOrgUnitJdbcRepository;
import org.nmcpye.datarun.etl.repository.DimTeamJdbcRepository;
import org.nmcpye.datarun.utils.UuidUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefTypeValueResolutionService {

    private final DimOptionJdbcRepository dimOptionRepo;
    private final DimOrgUnitJdbcRepository dimOrgUnitRepo;
    private final DimTeamJdbcRepository dimTeamJdbcRepo;

    public RefTypeValue resolve(TallCanonicalRow rc, String refType, @NotNull @Size(max = 11) String templateUid,
                                String activityUid, String submissionUid, String instanceKey, String optionSetUid, Instant startTime) {
        var token = rc.getValueText();
        var refTypeValueBuilder = RefTypeValue.builder()
            .ceId(UuidUtils.toUuidOrNull(rc.getCanonicalElementId()))
            .submissionUid(submissionUid)
            .instanceKey(instanceKey)
            .templateUid(templateUid)
            .rawValue(token)
            .refType(refType)
            .createdAt(startTime)
            .optionSetUid(optionSetUid);
        // 2) deterministic lookups
        if ("option".equalsIgnoreCase(refType)) {
            Optional<Map<String, Object>> opt;
            opt = dimOptionRepo.findByOptionSetAndToken(optionSetUid, token);
            if (opt.isPresent()) {
                String resolvedUid = (String) opt.get().getOrDefault("option_uid", null);
                return refTypeValueBuilder
                    .valueRefUid(resolvedUid)
                    .build();
            }
        } else if ("org_unit".equalsIgnoreCase(refType) || "orgunit".equalsIgnoreCase(refType)) {
            Optional<Map<String, Object>> ou = dimOrgUnitRepo.findByUid(token);
            if (ou.isPresent()) {
                String resolvedUid = (String) ou.get().getOrDefault("org_unit_uid", null);
                return refTypeValueBuilder
                    .valueRefUid(resolvedUid)
                    .build();
            }
            Optional<Map<String, Object>> ou2 = dimOrgUnitRepo.findByCodeOrUid(token);
            if (ou2.isPresent()) {
                String resolvedUid = (String) ou2.get().getOrDefault("org_unit_uid", null);
                return refTypeValueBuilder
                    .valueRefUid(resolvedUid)
                    .build();
            }
        } else if ("team".equalsIgnoreCase(refType)) {
            Optional<Map<String, Object>> ou = dimTeamJdbcRepo.findByUid(token);
            if (ou.isPresent()) {
                String resolvedUid = (String) ou.get().getOrDefault("team_uid", null);
                return refTypeValueBuilder
                    .valueRefUid(resolvedUid)
                    .build();
            }
            Optional<Map<String, Object>> ou2 = dimTeamJdbcRepo.findByActivityAndCodeOrUid(activityUid, token);
            if (ou2.isPresent()) {
                String resolvedUid = (String) ou2.get().getOrDefault("team_uid", null);
                return refTypeValueBuilder
                    .valueRefUid(resolvedUid)
                    .build();
            }
        }

        // 3) not found — persist miss
        return refTypeValueBuilder
            .valueRefUid(null)
            .build();
    }
}
