package org.nmcpye.datarun.etl.service.impl;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.common.uidgenerate.CodeGenerator;
import org.nmcpye.datarun.etl.dto.RefResolutionDto;
import org.nmcpye.datarun.etl.repository.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefResolutionService {

    private final RefResolutionJdbcRepository refResRepo;
    private final DimOptionJdbcRepository dimOptionRepo;
    private final DimOrgUnitJdbcRepository dimOrgUnitRepo;
    private final DimTeamJdbcRepository dimTeamJdbcRepo;
    private final SubmissionKeysJdbcRepository submissionKeysJdbcRepository;

    public final static String REF_RESOLUTION_CACHE_NAME = "refResolutionCache";

    /// @param resolvedUid 11-char uid or null
    /// @param confidence  0.0..1.0
    public record Resolution(String resolvedUid, double confidence, Instant resolvedAt) {
    }

    /**
     * Resolve a token for a given refType.
     * refType examples: "option", "org_unit", "team", "activity"
     * If optionSetUid provided, option lookup is constrained to that set.
     * <p>
     * This method is cached (Ehcache) to speed repeated lookups.
     */
    @Cacheable(value = REF_RESOLUTION_CACHE_NAME, key = "#refType + ':' + (#activityUid==null?'':#activityUid) + ':' +(#optionSetUid==null?'':#optionSetUid) + ':' + #token")
    public Resolution resolve(String token, String refType, String optionSetUid, String activityUid, String rawSource) {
        if (token == null || token.isBlank() || refType == null) return new Resolution(null, 0.0, Instant.now());

        // 1) check existing resolution table
        Optional<RefResolutionDto> existing = refResRepo.findLatestByRawAndType(token, refType);
        if (existing.isPresent()) {
            RefResolutionDto row = existing.get();
            double conf = row.getConfidence() == null ? 1.0 : row.getConfidence().doubleValue();
            Instant at = row.getResolvedAt() != null ? row.getResolvedAt().atZone(java.time.ZoneId.systemDefault()).toInstant() : Instant.now();
            return new Resolution(row.getResolvedUid(), conf, at);
        }

        // 2) deterministic lookups
        if ("option".equalsIgnoreCase(refType)) {
            Optional<Map<String, Object>> opt;
            if (optionSetUid != null) opt = dimOptionRepo.findByOptionSetAndToken(optionSetUid, token);
            else opt = dimOptionRepo.findByCodeOrUidOrNameVariants(token);
            if (opt.isPresent()) {
                String resolvedUid = (String) opt.get().getOrDefault("option_uid", null);
                persistResolution(token, rawSource, refType, resolvedUid, 1.0);
                return new Resolution(resolvedUid, 1.0, Instant.now());
            }
        } else if ("org_unit".equalsIgnoreCase(refType) || "orgunit".equalsIgnoreCase(refType)) {
            Optional<Map<String, Object>> ou = dimOrgUnitRepo.findByUid(token);
            if (ou.isPresent()) {
                String resolvedUid = (String) ou.get().getOrDefault("org_unit_uid", null);
                persistResolution(token, rawSource, refType, resolvedUid, 1.0);
                return new Resolution(resolvedUid, 1.0, Instant.now());
            }
            Optional<Map<String, Object>> ou2 = dimOrgUnitRepo.findByCodeOrUid(token);
            if (ou2.isPresent()) {
                String resolvedUid = (String) ou2.get().getOrDefault("org_unit_uid", null);
                persistResolution(token, rawSource, refType, resolvedUid, 1.0);
                return new Resolution(resolvedUid, 1.0, Instant.now());
            }
        } else if ("team".equalsIgnoreCase(refType)) {
            Optional<Map<String, Object>> ou = dimTeamJdbcRepo.findByUid(token);
            if (ou.isPresent()) {
                String resolvedUid = (String) ou.get().getOrDefault("team_uid", null);
                persistResolution(token, rawSource, refType, resolvedUid, 1.0);
                return new Resolution(resolvedUid, 1.0, Instant.now());
            }
            Optional<Map<String, Object>> ou2 = dimTeamJdbcRepo.findByActivityAndCodeOrUid(activityUid, token);
            if (ou2.isPresent()) {
                String resolvedUid = (String) ou2.get().getOrDefault("team_uid", null);
                persistResolution(token, rawSource, refType, resolvedUid, 1.0);
                return new Resolution(resolvedUid, 1.0, Instant.now());
            }
        }

        // 3) not found — persist miss
        persistResolution(token, rawSource, refType, null, 0.0);
        return new Resolution(null, 0.0, Instant.now());
    }

    private void persistResolution(String rawValue, String rawSource, String refType, String resolvedUid, double confidence) {
        String uid = CodeGenerator.nextUlid();
        Instant now = Instant.now();
        refResRepo.insertResolution(uid, rawValue, rawSource, refType, resolvedUid, confidence, now, null, null);
    }
}
