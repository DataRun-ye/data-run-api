package org.nmcpye.datarun.jpa.datatemplategenerator;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.datatemplateelement.FormSectionConf;
import org.nmcpye.datarun.jpa.datatemplate.CanonicalElement;
import org.nmcpye.datarun.jpa.datatemplate.TemplateElement;
import org.nmcpye.datarun.jpa.datatemplate.TemplateVersion;
import org.nmcpye.datarun.jpa.datatemplate.repository.MetadataUpsertService;
import org.nmcpye.datarun.jpa.datatemplate.repository.TemplateVersionRepository;
import org.nmcpye.datarun.jpa.option.OptionSet;
import org.nmcpye.datarun.jpa.option.repository.OptionSetRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Thin orchestrator that only composes small components. Pure generation — no database writes.
 * <p>
 * Usage:
 * List<TemplateElement> configs = generator.generate(templateUid, versionUid);
 * // persist configs using separate Publisher/Repository
 *
 * @author Hamza Assada
 * @since 09/09/2025
 */
@Service
@RequiredArgsConstructor
public class TemplateElementGeneratorService {
    private final TemplateVersionRepository versionRepository;
    private final FlatTemplateProcessor flatProcessor;
    private final TemplateElementBuilder elementBuilder;
    private final MetadataUpsertService metadataUpsertService;
    private final OptionSetRepository optionSetRepository;

    /**
     * Generates template elements and persists them via MetadataUpsertService (batch upsert).
     * This method is idempotent (uses deterministic UIDs) and safe for concurrent runs.
     */
    @Transactional
    public List<TemplateElement> generate(String templateUid, String versionUid) {
        Objects.requireNonNull(templateUid, "templateUid required");
        Objects.requireNonNull(versionUid, "versionUid required");

        TemplateVersion dtv = versionRepository.findByTemplateUidAndUid(templateUid, versionUid)
            .orElseThrow(() -> new IllegalArgumentException("template version not found"));

        FlatTemplateProcessor.TemplateFlatSnapshot snap = flatProcessor.process(dtv);
        MaterializedPathResolver resolver = new MaterializedPathResolver(snap.sectionByName);

        // 1) Build repeat configs (one per repeatable section)
        List<TemplateElement> out = new ArrayList<>(snap.fields.size() + snap.sectionByName.size());
        for (FormSectionConf section : snap.sectionByName.values()) {
            PathMetadata sectionMeta = resolver.resolveForSection(section);
            if (Boolean.TRUE.equals(section.getRepeatable())) {
                TemplateElement repeatCfg = elementBuilder.buildTemplateElementFromRepeat(section, sectionMeta, dtv);
                out.add(repeatCfg);
            }
        }

        // optionSetUid -> optionSetId
        Map<String, String> optionSetIdByUidMap = optionSetRepository.findAllByUidIn(snap.fields.stream()
                .map(FormDataElementConf::getOptionSet).filter(Objects::nonNull).collect(Collectors.toSet())).stream()
            .collect(Collectors.toMap(OptionSet::getUid, OptionSet::getId));
        // 2) Build field configs (single loop; previous code accidentally had this twice)
        Set<String> seen = new HashSet<>();
        for (FormDataElementConf f : snap.fields) {
            PathMetadata meta = resolver.resolveForField(f);

            if (!seen.add(meta.getJsonDataIdPath())) {
                throw new IllegalStateException("Duplicate idPath detected: " + meta.getJsonDataIdPath());
            }
            TemplateElement cfg = elementBuilder.buildTemplateElementFromField(f, meta, dtv);
            if(cfg.getOptionSetUid() != null) {
                cfg.setOptionSetId(optionSetIdByUidMap.get(cfg.getOptionSetUid()));
            }
            out.add(cfg);
        }

        // 3) Ensure every TemplateElement has a deterministic uid (DB requires it non-null/unique)
        //    and ensure canonicalElementUid exists (defensive).
        for (TemplateElement e : out) {
//            if (e.getUid() == null || e.getUid().isBlank()) {
//                e.setUid(generateShortElementUid(e.getTemplateUid(), e.getIdPath()));
//            }
            if (e.getCanonicalElementUid() == null || e.getCanonicalElementUid().isBlank()) {
                // fallback deterministic canonical uid built from canonicalPath + dataType + semanticType
                String fallbackKey = String.join("|", e.getTemplateUid(),
                    e.getCanonicalPath() == null ? "" : e.getCanonicalPath(),
                    e.getDataType() == null ? "" : e.getDataType().name(),
                    e.getSemanticType() == null ? "" : e.getSemanticType().name(), e.getCardinality());
                e.setCanonicalElementUid(canonicalUidFromStringAsUuid(fallbackKey));
            }
        }

        // 4) Build unique CanonicalElement list (one per canonicalElementUid)
        Map<String, CanonicalElement> canonicalByUid = new LinkedHashMap<>(); // preserve insertion order
        for (TemplateElement e : out) {
            final String ceUid = e.getCanonicalElementUid();
            CanonicalElement ce = canonicalByUid.get(ceUid);
            if (ce == null) {
                ce = CanonicalElement.builder()
                    .canonicalElementUid(ceUid)
                    .templateUid(e.getTemplateUid())
                    .preferredName(e.getName() != null ? e.getName() : "unnamed")
                    .dataType(e.getDataType())
                    .displayLabel(e.getDisplayLabel())
                    .semanticType(e.getSemanticType())
                    // canonicalPath in your entity was declared as JSON -- store single-path list as convenience
                    .canonicalPath(e.getCanonicalPath())
                    .cardinality(e.getCardinality())
                    .optionSetUid(e.getOptionSetUid())
                    .optionSetId(e.getOptionSetId())
                    .jsonDataPaths(Set.of(e.getJsonDataPath() == null ? "" : e.getJsonDataPath()))
                    .notes(null)
//                    .createdDate(Instant.now())
//                    .lastModifiedDate(Instant.now())
                    .build();
                canonicalByUid.put(ceUid, ce);
            } else {
                // merge jsonDataPath into canonical_candidates if not present
                String path = e.getJsonDataPath();
                if (path != null && !path.isBlank()) {
                    Set<String> candidates = Optional.ofNullable(ce.getJsonDataPaths()).orElse(new HashSet<>());
                    if (!candidates.contains(path)) {
                        candidates = new HashSet<>(candidates);
                        candidates.add(path);
                        ce.setJsonDataPaths(candidates);
                        ce.setDisplayLabel(e.getDisplayLabel());
                    }
                }
            }
        }

        List<CanonicalElement> canonicalList = new ArrayList<>(canonicalByUid.values());

        // 5) Upsert canonical elements first (FK referenced by template_element)
        metadataUpsertService.upsertCanonicalElements(canonicalList);

        // 6) Upsert template elements
        metadataUpsertService.upsertTemplateElements(out);

        // return the in-memory generated list (note: elements may not reflect DB default columns)
        return out;
    }
    // ---------- helpers ----------

    /**
     * Deterministic canonical UID (UUID name-based).
     */
    private static String canonicalUidFromStringAsUuid(String key) {
        if (key == null) key = "";
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
    }

    /**
     * Generate a short (11-char) deterministic uid for template_element.uid
     * based on templateUid + idPath. Produces [a-f0-9] substring (no dashes).
     */
    private static String generateShortElementUid(String templateUid, String idPath) {
        String seed = (templateUid == null ? "" : templateUid) + '|' + (idPath == null ? "" : idPath);
        String hex = canonicalUidFromStringAsUuid(seed).replace("-", "");
        // take first 11 chars. This gives 11 hex chars (safely within length), adjust if you want different base.
        return hex.substring(0, Math.min(11, hex.length()));
    }

//    @Transactional//(readOnly = true)
//    public List<TemplateElement> generate2(String templateUid, String versionUid) {
//        Objects.requireNonNull(templateUid, "templateUid required");
//        Objects.requireNonNull(versionUid, "versionUid required");
//
//        TemplateVersion dtv = versionRepository.findByTemplateUidAndUid(templateUid, versionUid)
//            .orElseThrow();
//        FlatTemplateProcessor.TemplateFlatSnapshot snap = flatProcessor.process(dtv);
//
//        // resolver needs sectionByName map
//        MaterializedPathResolver resolver = new MaterializedPathResolver(snap.sectionByName);
//
//        // produce repeat configs first (one per repeatable section)
//        List<TemplateElement> out = new ArrayList<>(snap.fields.size() + snap.sectionByName.size());
//        for (FormSectionConf section : snap.sectionByName.values()) {
//            PathMetadata sectionMeta = resolver.resolveForSection(section);
//            // create config only for repeatable sections (REPEAT elementKind)
//            if (Boolean.TRUE.equals(section.getRepeatable())) {
//                TemplateElement repeatCfg = elementBuilder.buildTemplateElementFromRepeat(section, sectionMeta, dtv);
//                out.add(repeatCfg);
//            }
//        }
//
//        // produce fields
//        Set<String> seen = new HashSet<>();
//        // produce field configs
//        for (FormDataElementConf f : snap.fields) {
//            PathMetadata meta = resolver.resolveForField(f);
//            if (!seen.add(meta.getJsonDataIdPath())) {
//                throw new IllegalStateException("Duplicate idPath detected: " + meta.getJsonDataIdPath());
//            }
//            TemplateElement cfg = elementBuilder.buildTemplateElementFromField(f, meta, dtv);
//            out.add(cfg);
//        }
//
//
//
//        final var ceUids = out.stream().map(TemplateElement::getCanonicalElementUid).collect(Collectors.toSet());
//
//        final var existingCanonicalElements = canonicalElementRepository.findDistinctByCanonicalElementUidIn(ceUids);
//
//        // Persist (delete existing, bulk insert)
//        final var ids = templateElementRepository.findIdsByTemplateUidAndTemplateVersionUid(templateUid, versionUid);
//
//        templateElementRepository.deleteAllByIdInBatch(ids);
//        templateElementRepository.persistAll(out);
//
//        return out;
//    }
}
