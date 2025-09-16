//package org.nmcpye.datarun.jpa.migration.templateversionmongo;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.nmcpye.datarun.jpa.datatemplate.DataTemplate;
//import org.nmcpye.datarun.jpa.datatemplate.TemplateVersion;
//import org.nmcpye.datarun.jpa.datatemplate.repository.DataTemplateRepository;
//import org.nmcpye.datarun.jpa.datatemplate.repository.TemplateVersionRepository;
//import org.nmcpye.datarun.mongo.datastagesubmission.repository.DataFormSubmissionRepository;
//import org.nmcpye.datarun.mongo.datatemplateversion.DataTemplateVersion;
//import org.nmcpye.datarun.mongo.datatemplateversion.repository.DataTemplateVersionRepository;
//import org.springframework.data.domain.Sort;
//import org.springframework.stereotype.Service;
//
//import java.util.*;
//import java.util.concurrent.TimeUnit;
//import java.util.stream.Collectors;
//
///**
// * @author Hamza Assada - 7amza.it@gmail.com
// * @since 24/08/2025
// */
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class TemplateVersionMigrationService_ {
//
//    private final DataTemplateVersionRepository mongoRepo; // Spring Data Mongo repository
//    private final DataTemplateRepository templateRepository; // Spring Data Mongo repository
//    private final TemplateVersionRepository pgRepo;       // JPA repository (Postgres)
//    private final TemplateVersionBatchSaver batchSaver;   // separate @Service for transactional saves
//    private final DataFormSubmissionRepository submissionRepo; // NEW: submissions repo
//
//    public void migrateAll(MigrationProperties props) throws InterruptedException {
//        log.info("Collecting versions that have submissions...");
//        List<String> versionUidsToMigrate = collectVersionUidsFromSubmissionsIncludingLatestForNoSubmissions();
//        if (versionUidsToMigrate.isEmpty()) {
//            log.info("No template versions found with submissions — nothing to migrate.");
//            return;
//        }
//        log.info("Found {} template-version uids with submissions.", versionUidsToMigrate.size());
//
//        long totalMigrated = 0L;
//        long totalScanned = 0L;
//
//        // process in batches of props.batchSize over the precomputed uid list
//        for (int offset = 0; offset < versionUidsToMigrate.size(); offset += props.getBatchSize()) {
//            if (props.getLimit() > 0 && totalMigrated >= props.getLimit()) {
//                log.info("Reached global migration limit {}. Stopping.", props.getLimit());
//                break;
//            }
//
//            int end = Math.min(offset + props.getBatchSize(), versionUidsToMigrate.size());
//            List<String> batchUids = versionUidsToMigrate.subList(offset, end);
//
//            // fetch the DataTemplateVersion docs for these uids
//            List<DataTemplateVersion> docs = mongoRepo.findAllByUidIn(batchUids,
//                Sort.by(Sort.Order.asc("templateUid"), Sort.Order.desc("versionNumber")));
//
//            if (docs.isEmpty()) {
//                log.debug("Batch offset={} returned no documents (uids may have been missing).", offset);
//                continue;
//            }
//
//            totalScanned += docs.size();
//
//            // map to TemplateVersion entities
//            List<TemplateVersion> entities = docs.stream()
//                .map(this::mapToEntityWithDefaults)
//                .collect(Collectors.toList());
//
//            // optionally skip existing ones by uid
//            if (props.isSkipExisting()) {
//                Set<String> uids = entities.stream().map(TemplateVersion::getUid).collect(Collectors.toSet());
//                List<String> present = pgRepo.findAllByUidIn(uids).stream()
//                    .map(TemplateVersion::getUid).toList();
//                if (!present.isEmpty()) {
//                    Set<String> presentSet = new HashSet<>(present);
//                    entities = entities.stream().filter(e -> !presentSet.contains(e.getUid())).collect(Collectors.toList());
//                }
//            }
//
//            if (entities.isEmpty()) {
//                log.info("Batch offset={} : no new entities to save (all present or filtered).", offset);
//                continue;
//            }
//
//            // attempt save with retries (preserve your existing retry logic)
//            int attempt = 0;
//            boolean saved = false;
//            Exception lastEx = null;
//            while (!saved && attempt < props.getMaxRetries()) {
//                attempt++;
//                try {
//                    List<TemplateVersion> savedEntities = batchSaver.saveBatch(entities);
//                    log.info("Saved batch offset={} count={} (firstUid={})", offset, savedEntities.size(), savedEntities.get(0).getUid());
//                    totalMigrated += savedEntities.size();
//                    saved = true;
//                } catch (Exception e) {
//                    lastEx = e;
//                    log.warn("Attempt {} to save batch at offset {} failed: {}", attempt, offset, e.getMessage());
//                    if (attempt < props.getMaxRetries()) {
//                        log.info("Retrying after {} ms...", props.getRetryBackoffMs());
//                        TimeUnit.MILLISECONDS.sleep(props.getRetryBackoffMs());
//                    }
//                }
//            }
//
//            if (!saved) {
//                log.error("Failed to save batch at offset {} after {} attempts.", offset, props.getMaxRetries());
//                if (!props.isDryRun()) {
//                    throw new RuntimeException("Batch save failed", lastEx);
//                }
//            }
//
//            // optional limit stop
//            if (props.getLimit() > 0 && totalMigrated >= props.getLimit()) {
//                log.info("Reached migration limit after offset {}. Migrated {} entities.", offset, totalMigrated);
//                break;
//            }
//        }
//
//        log.info("Migration finished. scanned={}, migrated={}", totalScanned, totalMigrated);
//    }
//
////    public void migrateAll(MigrationProperties props) throws InterruptedException {
////        int page = 0;
////        long totalMigrated = 0L;
////        long totalScanned = 0L;
////
////        while (true) {
////            PageRequest pageable = PageRequest.of(page, props.getBatchSize(),
////                    Sort.by(Sort.Order.asc("templateUid"), Sort.Order.desc("versionNumber")));
////
////            Page<DataTemplateVersion> mongoPage = mongoRepo.findAll(pageable);
////            List<DataTemplateVersion> docs = mongoPage.getContent();
////
////            if (docs.isEmpty()) {
////                log.debug("No more documents in Mongo. ending pagination (page={}).", page);
////                break;
////            }
////
////            totalScanned += docs.size();
////
////            // optional resume filter
////            if (props.getResumeAfterUid() != null) {
////                docs = docs.stream()
////                        .filter(d -> d.getUid().compareTo(props.getResumeAfterUid()) > 0)
////                        .toList();
////            }
////
////            // apply overall limit if configured
////            if (props.getLimit() > 0 && totalMigrated >= props.getLimit()) {
////                log.info("Reached global migration limit {}. Stopping.", props.getLimit());
////                break;
////            }
////
////            // map and prepare entities
////            List<TemplateVersion> entities = docs.stream()
////                    .map(this::mapToEntityWithDefaults)
////                    .collect(Collectors.toList());
////
////            // optionally skip already existing by id (idempotency)
////            if (props.isSkipExisting()) {
////                Set<String> uids = entities.stream().map(TemplateVersion::getUid).collect(Collectors.toSet());
////                List<String> present = pgRepo.findAllByUidIn(uids).stream()
////                        .map(TemplateVersion::getUid).toList();
////                if (!present.isEmpty()) {
////                    Set<String> presentSet = new HashSet<>(present);
////                    entities = entities.stream().filter(e -> !presentSet.contains(e.getUid())).collect(Collectors.toList());
////                }
////            }
////
////            if (entities.isEmpty()) {
////                log.info("Batch {}: no new entities to save (all present or filtered). Scanned so far={}.", page, totalScanned);
////                page++;
////                continue;
////            }
////
////            // attempt save with retries
////            int attempt = 0;
////            boolean saved = false;
////            Exception lastEx = null;
////            while (!saved && attempt < props.getMaxRetries()) {
////                attempt++;
////                try {
////                    if (props.isDryRun()) {
////                        log.info("Dry-run: would save {} entities for page {} (skipping actual DB writes)", entities.size(), page);
////                        saved = true; // treat dry-run as success
////                    } else {
////                        List<TemplateVersion> savedEntities = batchSaver.saveBatch(entities);
////                        log.info("Saved batch page={} count={} (firstUid={})", page, savedEntities.size(), savedEntities.get(0).getUid());
////                        totalMigrated += savedEntities.size();
////                        saved = true;
////                    }
////                } catch (Exception e) {
////                    lastEx = e;
////                    log.warn("Attempt {} to save batch {} failed: {}", attempt, page, e.getMessage());
////                    if (attempt < props.getMaxRetries()) {
////                        log.info("Retrying after {} ms...", props.getRetryBackoffMs());
////                        TimeUnit.MILLISECONDS.sleep(props.getRetryBackoffMs());
////                    }
////                }
////            }
////
////            if (!saved) {
////                log.error("Failed to save batch {} after {} attempts.", page, props.getMaxRetries());
////                if (!props.isDryRun()) {
////                    throw new RuntimeException("Batch save failed", lastEx);
////                }
////            }
////
////            page++;
////
////            // optional global stop condition for testing
////            if (props.getLimit() > 0 && totalMigrated >= props.getLimit()) {
////                log.info("Reached migration limit after page {}. Migrated {} entities.", page, totalMigrated);
////                break;
////            }
////        }
////
////        log.info("Migration finished. scanned={}, migrated={}", totalScanned, totalMigrated);
////    }
//
//    /**
//     * Collect DataTemplateVersion.uids that correspond to template-version pairs that have submissions.
//     */
//    private List<String> collectVersionUidsFromSubmissionsIncludingLatestForNoSubmissions() {
//        // 1) All (form,version) pairs that appear in submissions
//        List<DataFormSubmissionRepository.FormVersionPair> pairs = submissionRepo.findDistinctFormAndVersion();
//
//        Set<String> uids = new LinkedHashSet<>(); // preserve deterministic order
//        Set<String> formsWithSubs = new HashSet<>();
//
//        if (pairs != null) {
//            for (DataFormSubmissionRepository.FormVersionPair p : pairs) {
//                if (p.getForm() == null || p.getCurrentVersion() == null) continue;
//                formsWithSubs.add(p.getForm());
//                mongoRepo.findByTemplateUidAndVersionNumber(p.getForm(), p.getCurrentVersion())
//                    .ifPresent(dtv -> uids.add(dtv.getUid()));
//            }
//        }
//
//        List<DataTemplate> templates = templateRepository.findAll();
//        List<String> latestVersions = mongoRepo.
//            findAllByUidIn(templates
//                .stream().map(DataTemplate::getVersionUid)
//                .toList()).stream().map(DataTemplateVersion::getUid).toList();
//
//        // 2) Find templates that have NO submissions and add their latest version only
//        for (String tUid : latestVersions) {
//            if (!formsWithSubs.contains(tUid)) {
//                uids.add(tUid);
//            }
//        }
//
//        return new ArrayList<>(uids);
//    }
//
//    private TemplateVersion mapToEntityWithDefaults(DataTemplateVersion src) {
//        final var dataTemplate = templateRepository.findByUid(src.getTemplateUid()).orElseThrow();
//        TemplateVersion e = TemplateVersion.builder()
//            .templateUid(dataTemplate.getUid())
//            .dataTemplate(dataTemplate)
//            .versionNumber(src.getVersionNumber())
//            .uid(src.getUid())
//            .fields(src.getFields())
//            .sections(src.getSections())
//            .build();
//
//        return e;
//    }
//}
//
