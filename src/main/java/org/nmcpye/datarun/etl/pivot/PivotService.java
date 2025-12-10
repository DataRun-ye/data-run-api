package org.nmcpye.datarun.etl.pivot;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.jpa.datatemplate.repository.DataTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PivotService {
    private static final Logger log = LoggerFactory.getLogger(PivotService.class);

    private final CeMetadataRepository ceRepo;
    private final SqlGenerator sqlGen;
    private final DdlExecutor ddl;
    private final ValidationService validator;
    private final PivotManifestRepository manifestRepo;
    private final Naming naming;
    private final DataTemplateRepository templateRepository;

    public BuildResponse buildTemplate(String uid) {
        Instant start = Instant.now();
        var template = templateRepository.findFirstByCodeOrUidOrId(uid, uid, uid)
            .orElseThrow(() -> new IllegalArgumentException("Unknown template: " + uid));

        var templateUid = template.getUid();

        manifestRepo.startBuild(templateUid, start);

        List<CanonicalElement> ces = ceRepo.getElementsForTemplate(templateUid);
        if (ces == null || ces.isEmpty()) {
            String msg = "No CE metadata found for template=" + templateUid;
            manifestRepo.failBuild(templateUid, msg);
            return BuildResponse.failure(msg);
        }

        // compute canonical base table name (pivot.fact_<sanitized>)
        String baseFq = naming.fqFactTableForTemplate(templateUid); // pivot.fact_<sanitized>
        // SqlGenerator is expected to create <baseFq>_new
        String createSql = sqlGen.buildTemplateCreateSqlForBase(baseFq, templateUid, ces);

        try {
            log.info("pivot: start build template={}, baseFq={}, sql-length={}", templateUid, baseFq, createSql.length());
            ddl.execute(createSql, Naming.newName(baseFq));
            log.info("pivot: DDL created (committed): template={}, checking validation", templateUid);

            log.debug("pivot: running validator.validateTemplateTable for baseFq={}, template={}", baseFq, templateUid);
            ValidationResult vr = validator.validateTemplateTable(baseFq, templateUid);
            if (!vr.passed()) {
                manifestRepo.failBuild(templateUid, vr);
                return BuildResponse.failure("Validation failed", vr, null);
            }

            log.info("pivot: validation passed, performing atomic swap for template={}", templateUid);
            ddl.atomicSwapTemplate(templateUid);
            log.info("pivot: atomic swap completed for template={}", templateUid);
            log.info("pivot: start creating indexes template={}, baseFq={}, sql-length={}", templateUid, baseFq, createSql.length());
            log.info("pivot: indexes created for template={}, baseFq={}", templateUid, baseFq);
            Duration duration = Duration.between(start, Instant.now());
            manifestRepo.completeBuild(templateUid, duration);
            return BuildResponse.success(duration.getSeconds());
        } catch (Exception ex) {
            manifestRepo.failBuild(templateUid, ex);
            log.error("pivot: build failed for template={}, reason={}", templateUid, ex.getMessage(), ex);
            return BuildResponse.failure(ex);
        }
    }

//    public BuildResponse buildActivity(String activityId) {
//        Instant start = Instant.now();
//        manifestRepo.startBuild(activityId, start);
//
//        List<CanonicalElement> ces = ceRepo.getElementsForActivity(activityId);
//        if (ces == null || ces.isEmpty()) {
//            String msg = "No CE metadata found for activity=" + activityId;
//            manifestRepo.failBuild(activityId, msg);
//            return BuildResponse.failure(msg);
//        }
//
//        // For activity-level builds we create two tables: base and base_repeats
//        String sanitized = Naming.sanitize(activityId);
//        String baseFq = "pivot.fact_" + sanitized;
//        String baseFqRepeats = baseFq + "_repeats";
//
//        // Expect sqlGen to create baseFq_new and baseFq_repeats_new
//        String submissionSql = sqlGen.buildSubmissionCreateSqlForBase(baseFq, activityId, ces);
//        String repeatsSql = sqlGen.buildRepeatsCreateSqlForBase(baseFqRepeats, activityId, ces);
//
//        try {
//            log.info("Starting create for activity {}", activityId);
//            ddl.execute(submissionSql);
//            ddl.execute(repeatsSql);
//
//            ValidationResult vr1 = validator.validateSubmissionTable(activityId);
//            ValidationResult vr2 = validator.validateRepeatsTable(activityId);
//
//            if (!vr1.passed() || !vr2.passed()) {
//                manifestRepo.failBuild(activityId, vr1, vr2);
//                return BuildResponse.failure("Validation failed", vr1, vr2);
//            }
//
//            // this method atomically swaps both main and repeats tables
//            ddl.atomicSwapActivity(activityId);
//
//            Duration duration = Duration.between(start, Instant.now());
//            manifestRepo.completeBuild(activityId, duration, vr1, vr2);
//            return BuildResponse.success(duration.getSeconds());
//        } catch (Exception ex) {
//            log.error("Build failed for activity {}", activityId, ex);
//            manifestRepo.failBuild(activityId, ex);
//            return BuildResponse.failure(ex);
//        }
//    }

    public StatusResponse getStatus(String activity) {
        return manifestRepo.getStatus(activity);
    }
}

/// / File: src/main/resources/application.yml
//spring:
//datasource:
//url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:superset}
//username: ${DB_USER:pivot_builder}
//password: ${DB_PASS:changeme}
//driver-class-name: org.postgresql.Driver
//
//pivot:
//schema: analytics
//manifest-table: pivot_manifest
//temp-suffix: _new
//keep-old-days: 1
//default-validation-threshold: 0.9
//
/// / END OF FILE
