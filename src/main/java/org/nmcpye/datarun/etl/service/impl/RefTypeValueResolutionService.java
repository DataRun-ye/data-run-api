package org.nmcpye.datarun.etl.service.impl;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.etl.repository.DimOptionJdbcRepository;
import org.nmcpye.datarun.etl.repository.DimOrgUnitJdbcRepository;
import org.nmcpye.datarun.etl.repository.DimTeamJdbcRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefTypeValueResolutionService {

    private final DimOptionJdbcRepository dimOptionRepo;
    private final DimOrgUnitJdbcRepository dimOrgUnitRepo;
    private final DimTeamJdbcRepository dimTeamJdbcRepo;

    public String resolve(String token, String refType, String optionSetUid, String activityUid) {
//        var token = rc.getValueText();
//        var refTypeValueBuilder = RefTypeValue.builder()
//            .ceId(UuidUtils.toUuidOrNull(rc.getCanonicalElementId()))
//            .submissionUid(submissionUid)
//            .instanceKey(instanceKey)
//            .templateUid(templateUid)
//            .rawValue(token)
//            .refType(refType)
//            .createdAt(startTime)
//            .optionSetUid(optionSetUid);
        // 2) deterministic lookups
        String resolvedUid = null;
        if ("option".equalsIgnoreCase(refType)) {
            resolvedUid = resolveOption(token, optionSetUid);
        } else if ("org_unit".equalsIgnoreCase(refType) || "orgunit".equalsIgnoreCase(refType)) {
            resolvedUid = resolveOrgUnit(token);
        } else if ("team".equalsIgnoreCase(refType)) {
            resolvedUid = resolveTeam(token, activityUid);
        }

        // 3) not found — persist miss
        return resolvedUid;
    }

    String resolveOption(String token, String optionSetUid) {
        Map<String, Object> opt = dimOptionRepo.findByOptionSetAndToken(optionSetUid, token).orElseGet(HashMap::new);
        return (String) opt.getOrDefault("option_uid", null);
    }

    String resolveOrgUnit(String token) {
        Map<String, Object> ou = dimOrgUnitRepo.findByCodeOrUid(token).orElseGet(HashMap::new);
        return (String) ou.getOrDefault("org_unit_uid", null);
    }

    String resolveTeam(String token, String activityUid) {
        Optional<Map<String, Object>> team = dimTeamJdbcRepo.findByUid(token);
        String resolvedUid = null;
        if (team.isPresent()) {
            resolvedUid = (String) team.get().getOrDefault("team_uid", null);
        }

        if (activityUid != null) {
            Optional<Map<String, Object>> ou2 = dimTeamJdbcRepo.findByActivityAndCodeOrUid(activityUid, token);
            if (ou2.isPresent()) {
                resolvedUid = (String) ou2.get().getOrDefault("team_uid", null);
            }
        }

        return resolvedUid;
    }
}
