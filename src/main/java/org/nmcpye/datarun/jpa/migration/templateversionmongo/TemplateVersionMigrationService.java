package org.nmcpye.datarun.jpa.migration.templateversionmongo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.jpa.datatemplate.TemplateVersion;
import org.nmcpye.datarun.jpa.datatemplate.repository.DataTemplateRepository;
import org.nmcpye.datarun.jpa.datatemplate.repository.TemplateVersionRepository;
import org.nmcpye.datarun.mongo.datatemplateversion.DataTemplateVersion;
import org.nmcpye.datarun.mongo.datatemplateversion.repository.DataTemplateVersionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Hamza Assada - 7amza.it@gmail.com
 * @since 24/08/2025
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateVersionMigrationService {

    private final DataTemplateVersionRepository mongoRepo; // Spring Data Mongo repository
    private final DataTemplateRepository templateRepository; // Spring Data Mongo repository
    private final TemplateVersionRepository pgRepo;       // JPA repository (Postgres)
    private final TemplateVersionBatchSaver batchSaver;   // separate @Service for transactional saves

    public void migrateAll(MigrationProperties props) throws InterruptedException {
        int page = 0;
        long totalMigrated = 0L;
        long totalScanned = 0L;

        while (true) {
            PageRequest pageable = PageRequest.of(page, props.getBatchSize(),
                    Sort.by(Sort.Order.asc("templateUid"), Sort.Order.desc("versionNumber")));

            Page<DataTemplateVersion> mongoPage = mongoRepo.findAll(pageable);
            List<DataTemplateVersion> docs = mongoPage.getContent();

            if (docs.isEmpty()) {
                log.debug("No more documents in Mongo. ending pagination (page={}).", page);
                break;
            }

            totalScanned += docs.size();

            // optional resume filter
            if (props.getResumeAfterUid() != null) {
                docs = docs.stream()
                        .filter(d -> d.getUid().compareTo(props.getResumeAfterUid()) > 0)
                        .toList();
            }

            // apply overall limit if configured
            if (props.getLimit() > 0 && totalMigrated >= props.getLimit()) {
                log.info("Reached global migration limit {}. Stopping.", props.getLimit());
                break;
            }

            // map and prepare entities
            List<TemplateVersion> entities = docs.stream()
                    .map(this::mapToEntityWithDefaults)
                    .collect(Collectors.toList());

            // optionally skip already existing by id (idempotency)
            if (props.isSkipExisting()) {
                Set<String> uids = entities.stream().map(TemplateVersion::getUid).collect(Collectors.toSet());
                List<String> present = pgRepo.findAllByUidIn(uids).stream()
                        .map(TemplateVersion::getUid).toList();
                if (!present.isEmpty()) {
                    Set<String> presentSet = new HashSet<>(present);
                    entities = entities.stream().filter(e -> !presentSet.contains(e.getUid())).collect(Collectors.toList());
                }
            }

            if (entities.isEmpty()) {
                log.info("Batch {}: no new entities to save (all present or filtered). Scanned so far={}.", page, totalScanned);
                page++;
                continue;
            }

            // attempt save with retries
            int attempt = 0;
            boolean saved = false;
            Exception lastEx = null;
            while (!saved && attempt < props.getMaxRetries()) {
                attempt++;
                try {
                    if (props.isDryRun()) {
                        log.info("Dry-run: would save {} entities for page {} (skipping actual DB writes)", entities.size(), page);
                        saved = true; // treat dry-run as success
                    } else {
                        List<TemplateVersion> savedEntities = batchSaver.saveBatch(entities);
                        log.info("Saved batch page={} count={} (firstUid={})", page, savedEntities.size(), savedEntities.get(0).getUid());
                        totalMigrated += savedEntities.size();
                        saved = true;
                    }
                } catch (Exception e) {
                    lastEx = e;
                    log.warn("Attempt {} to save batch {} failed: {}", attempt, page, e.getMessage());
                    if (attempt < props.getMaxRetries()) {
                        log.info("Retrying after {} ms...", props.getRetryBackoffMs());
                        TimeUnit.MILLISECONDS.sleep(props.getRetryBackoffMs());
                    }
                }
            }

            if (!saved) {
                log.error("Failed to save batch {} after {} attempts.", page, props.getMaxRetries());
                if (!props.isDryRun()) {
                    throw new RuntimeException("Batch save failed", lastEx);
                }
            }

            page++;

            // optional global stop condition for testing
            if (props.getLimit() > 0 && totalMigrated >= props.getLimit()) {
                log.info("Reached migration limit after page {}. Migrated {} entities.", page, totalMigrated);
                break;
            }
        }

        log.info("Migration finished. scanned={}, migrated={}", totalScanned, totalMigrated);
    }

    private TemplateVersion mapToEntityWithDefaults(DataTemplateVersion src) {
        final var dataTemplate = templateRepository.findByUid(src.getTemplateUid()).orElseThrow();
        TemplateVersion e = TemplateVersion.builder()
                .templateUid(dataTemplate.getUid())
                .dataTemplate(dataTemplate)
                .versionNumber(src.getVersionNumber())
                .uid(src.getUid())
                .fields(src.getFields())
                .sections(src.getSections())
                .build();

        return e;
    }
}

