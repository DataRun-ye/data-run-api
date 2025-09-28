//package org.nmcpye.datarun.jpa.dataingestion;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
//import org.nmcpye.datarun.jpa.datatemplate.TemplateVersion;
//import org.nmcpye.datarun.jpa.datatemplate.repository.TemplateVersionRepository;
//import org.nmcpye.datarun.jpa.etl.repository.ElementDataValueRepository;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.dao.DuplicateKeyException;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.Instant;
//import java.util.*;
//
///**
// * Production-grade ingest service for converting UI-nested submissions into canonical extraction artifacts.
// *
// * Design notes (concise):
// * - Raw submissions are stored immutable in MongoDB (fast document store, audit-friendly).
// * - Canonical, flattened artifacts (submission_field, repeat_instance, element_catalog) are stored in Postgres via JPA.
// * - Idempotency: submission_id is the idempotency key. We try to insert raw doc; DuplicateKey => someone retried.
// * - We use a Postgres advisory lock (by submission_id hash) in the transactional section to avoid concurrent processors writing canonical rows twice.
// * - Template metadata (template_version) is authoritative and immutable; used to map paths -> element_id.
// * - on error we update raw doc ingest status & error details for operators & retries.
// */
//@Service
//public class IngestService {
//
//    private static final Logger log = LoggerFactory.getLogger(IngestService.class);
//
//    private final DataSubmissionRepository submissionRepository;
//    private final TemplateVersionRepository templateVersionRepo;
//    private final ElementCatalogRepository elementCatalogRepo;
//    private final RepeatInstanceRepository repeatInstanceRepo;
//    private final ElementDataValueRepository dataValueRepository;
//    private final JdbcTemplate jdbcTemplate;
//    private final ObjectMapper objectMapper;
//
//    public IngestService(
//        DataSubmissionRepository submissionRepository,
//        TemplateVersionRepository templateVersionRepo,
//        ElementCatalogRepository elementCatalogRepo,
//        RepeatInstanceRepository repeatInstanceRepo,
//        ElementDataValueRepository dataValueRepository,
//        JdbcTemplate jdbcTemplate,
//        ObjectMapper objectMapper
//    ) {
//        this.submissionRepository = submissionRepository;
//        this.templateVersionRepo = templateVersionRepo;
//        this.elementCatalogRepo = elementCatalogRepo;
//        this.repeatInstanceRepo = repeatInstanceRepo;
//        this.dataValueRepository = dataValueRepository;
//        this.jdbcTemplate = jdbcTemplate;
//        this.objectMapper = objectMapper;
//    }
//
//    /**
//     * Top-level ingest entry. Idempotent w.r.t submissionId.
//     */
//    public IngestResult ingest(RawSubmissionDTO dto) {
//        Instant startedAt = Instant.now();
//        RawSubmissionDoc rawDoc = RawSubmissionDoc.fromDto(dto);
//
//        try {
//            // 1) persist raw submission to Mongo (immutable). If it exists, load it for reuse.
//            saveRawSubmissionIfNotExists(rawDoc);
//        } catch (DuplicateKeyException dk) {
//            log.info("Duplicate raw submission insert for {}, treating as idempotent retry", dto.getSubmissionId());
//            rawDoc = submissionRepository.findById(dto.getSubmissionId()).orElseThrow(() -> new IllegalStateException("Raw submission disappeared after duplicate key"));
//            // If it was already processed successfully, return success immediately
//            if (rawDoc.getIngestStatus() == IngestStatus.PROCESSED) {
//                return IngestResult.processed(rawDoc.getSubmissionId(), rawDoc.getProcessedAt());
//            }
//        } catch (Exception ex) {
//            log.error("Failed to persist raw submission {}", dto.getSubmissionId(), ex);
//            // Best-effort: persist an error flag if possible, then rethrow
//            try { submissionRepository.save(RawSubmissionDoc.errorFromDto(dto, ex)); } catch (Exception ignore) { /* ignore */ }
//            return IngestResult.failed(dto.getSubmissionId(), ex.getMessage());
//        }
//
//        // 2) Load template metadata (Postgres)
//        TemplateVersion templateVersion = templateVersionRepo.findById(rawDoc.getMetadataVersionId())
//            .orElseThrow(() -> new MissingTemplateMetadataException("Missing template metadata: " + rawDoc.getMetadataVersionId()));
//
//        // 3) Process canonicalization inside a Postgres transaction and advisory lock to ensure single writer semantics.
//        try {
//            processCanonicalizationTransactional(rawDoc, templateVersion);
//            // mark processed in raw doc
//            rawDoc.setIngestStatus(IngestStatus.PROCESSED);
//            rawDoc.setProcessedAt(Instant.now());
//            submissionRepository.save(rawDoc);
//            return IngestResult.processed(rawDoc.getSubmissionId(), rawDoc.getProcessedAt());
//        } catch (Exception ex) {
//            log.error("Failure processing canonicalization for submission {}", rawDoc.getSubmissionId(), ex);
//            // mark failed with reason
//            rawDoc.setIngestStatus(IngestStatus.FAILED);
//            rawDoc.setErrorMessage(truncate(ex.getMessage(), 2000));
//            submissionRepository.save(rawDoc);
//            return IngestResult.failed(rawDoc.getSubmissionId(), ex.getMessage());
//        }
//    }
//
//    private void saveRawSubmissionIfNotExists(RawSubmissionDoc rawDoc) {
//        submissionRepository.insert(rawDoc); // throws DuplicateKeyException if id exists
//    }
//
//    /**
//     * Core transactional method that populates repeat_instance and submission_field rows.
//     * We acquire a Postgres advisory lock to prevent other workers processing the same submission concurrently.
//     */
//    @Transactional(transactionManager = "transactionManager")
//    public void processCanonicalizationTransactional(RawSubmissionDoc rawDoc, TemplateVersion templateVersion) {
//        // Acquire an advisory lock scoped to this submission id - ensures single writer for the Postgres segment.
//        acquirePgAdvisoryLock(rawDoc.getSubmissionId());
//
//        // Idempotency: if submission_field rows already exist for this submission, treat as success and return.
//        boolean alreadyProcessed = dataValueRepository.existsBySubmissionId(rawDoc.getSubmissionId());
//        if (alreadyProcessed) {
//            log.info("Submission {} already has canonical rows, skipping canonicalization", rawDoc.getSubmissionId());
//            return;
//        }
//
//        // Traverse raw payload and build canonical rows
//        List<ElementValue> rows = new ArrayList<>();
//        Map<String, RepeatInstance> repeatRegistry = new HashMap<>();
//
//        JsonNode payload = rawDoc.getRawPayload();
//        traverseAndExtract(payload, "root", rawDoc.getSubmissionId(), templateVersion, repeatRegistry, rows);
//
//        // Persist repeat instances first (if any)
//        if (!repeatRegistry.isEmpty()) {
//            repeatInstanceRepo.saveAll(repeatRegistry.values());
//        }
//
//        // Persist submission fields in batch
//        if (!rows.isEmpty()) {
//            dataValueRepository.saveAll(rows);
//        }
//    }
//
//    private void acquirePgAdvisoryLock(UUID submissionId) {
//        // Use hashtext on the UUID string to derive a consistent int64 key via Postgres function.
//        // Note: This uses pg_advisory_xact_lock which is released at transaction end.
//        String sql = "SELECT pg_advisory_xact_lock(hashtext(?))";
//        jdbcTemplate.queryForObject(sql, Integer.class, submissionId.toString());
//    }
//
//    private void traverseAndExtract(JsonNode node,
//                                    String path,
//                                    UUID submissionId,
//                                    TemplateVersion templateVersion,
//                                    Map<String, RepeatInstance> repeatRegistry,
//                                    List<ElementValue> rows) {
//        if (node == null || node.isNull()) return;
//
//        // Check if this path is a repeat according to template metadata
//        Optional<RepeatDefinition> repeatDefOpt = templateVersion.findRepeatByPath(path);
//        if (repeatDefOpt.isPresent() && node.isArray()) {
//            RepeatDefinition repeatDef = repeatDefOpt.get();
//            int index = 0;
//            for (JsonNode item : node) {
//                String clientRid = findClientRid(item);
//                String key = path + "|" + (clientRid != null ? clientRid : index);
//                RepeatInstance rinst = repeatRegistry.get(key);
//                if (rinst == null) {
//                    rinst = RepeatInstance.create(UUID.randomUUID(), submissionId, path, clientRid, index);
//                    repeatRegistry.put(key, rinst);
//                }
//                String childPath = path + "[rid=" + (clientRid != null ? clientRid : index) + "]";
//                traverseAndExtract(item, childPath, submissionId, templateVersion, repeatRegistry, rows);
//                index++;
//            }
//            return;
//        }
//
//        if (node.isObject()) {
//            Iterator<Map.Entry<String, JsonNode>> it = node.fields();
//            while (it.hasNext()) {
//                Map.Entry<String, JsonNode> f = it.next();
//                String childName = f.getKey();
//                JsonNode childNode = f.getValue();
//                String nextPath = path.equals("root") ? childName : path + "." + childName;
//                traverseAndExtract(childNode, nextPath, submissionId, templateVersion, repeatRegistry, rows);
//            }
//            return;
//        }
//
//        if (node.isValueNode()) {
//            // leaf: resolve element metadata by semantic path (element name)
//            Optional<ElementCatalog> elementOpt = elementCatalogRepo.findByTemplateVersionIdAndName(templateVersion.getTemplateVersionId(), path);
//            if (!elementOpt.isPresent()) {
//                log.debug("No element metadata for path {} (submission {}) - skipping value: {}", path, submissionId, node.asText());
//                return;
//            }
//            ElementCatalog element = elementOpt.get();
//
//            ElementValue row = new ElementValue();
//            row.setSubmissionId(submissionId);
//            row.setElementId(element.getElementId());
//            row.setPath(path);
//            // find any repeat_instance if path contains [rid=...]
//            String rid = extractRidFromPath(path);
//            if (rid != null) {
//                // find matching repeat instance id in DB or registry (registry will be persisted earlier in this tx)
//                // We saved RepeatInstance objects in repeatRegistry with key = repeatPath|ridOrIndex
//                String repeatPath = path.substring(0, path.indexOf('['));
//                String registryKey = repeatPath + "|" + rid;
//                RepeatInstance r = repeatRegistry.get(registryKey);
//                if (r != null) row.setRepeatInstanceId(r.getRepeatInstanceId());
//            }
//
//            row.setValueText(node.asText());
//            row.setValueJson(node.isContainerNode() ? node : null);
//            row.setValueType(element.getType());
//            row.setMetadataVersionId(UUID.fromString(templateVersion.getTemplateVersionId()));
//
//            rows.add(row);
//        }
//    }
//
//    private String findClientRid(JsonNode item) {
//        if (item.has("_rid")) {
//            return item.get("_rid").asText();
//        }
//        return null;
//    }
//
//    private String extractRidFromPath(String path) {
//        int start = path.indexOf("[rid=");
//        if (start == -1) return null;
//        int end = path.indexOf(']', start);
//        if (end == -1) return null;
//        return path.substring(start + 5, end);
//    }
//
//    private static String truncate(String s, int max) {
//        if (s == null) return null;
//        return s.length() <= max ? s : s.substring(0, max);
//    }
//
//    // --- DTOs, entities, repos & small helpers below (kept in same file for sample convenience) ---
//
//    public static class IngestResult {
//        private final UUID submissionId;
//        private final boolean ok;
//        private final Instant processedAt;
//        private final String errorMessage;
//
//        private IngestResult(UUID submissionId, boolean ok, Instant processedAt, String errorMessage) {
//            this.submissionId = submissionId;
//            this.ok = ok;
//            this.processedAt = processedAt;
//            this.errorMessage = errorMessage;
//        }
//
//        public static IngestResult processed(UUID id, Instant at) { return new IngestResult(id, true, at, null); }
//        public static IngestResult failed(UUID id, String error) { return new IngestResult(id, false, null, error); }
//
//        // getters omitted for brevity
//    }
//}
//
//
