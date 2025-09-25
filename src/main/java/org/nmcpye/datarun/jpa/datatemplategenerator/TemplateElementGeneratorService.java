package org.nmcpye.datarun.jpa.datatemplategenerator;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.datatemplateelement.FormSectionConf;
import org.nmcpye.datarun.jpa.datatemplate.TemplateElement;
import org.nmcpye.datarun.jpa.datatemplate.TemplateVersion;
import org.nmcpye.datarun.jpa.datatemplate.repository.TemplateElementRepository;
import org.nmcpye.datarun.jpa.datatemplate.repository.TemplateVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

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
    private final TemplateElementRepository templateElementRepository;


    @Transactional//(readOnly = true)
    public List<TemplateElement> generate(String templateUid, String versionUid) {
        Objects.requireNonNull(templateUid, "templateUid required");
        Objects.requireNonNull(versionUid, "versionUid required");

        TemplateVersion dtv = versionRepository.findByTemplateUidAndUid(templateUid, versionUid)
            .orElseThrow();
        FlatTemplateProcessor.TemplateFlatSnapshot snap = flatProcessor.process(dtv);

        // resolver needs sectionByName map
        MaterializedPathResolver resolver = new MaterializedPathResolver(snap.sectionByName);

        // produce repeat configs first (one per repeatable section)
        List<TemplateElement> out = new ArrayList<>(snap.fields.size() + snap.sectionByName.size());
        for (FormSectionConf section : snap.sectionByName.values()) {
            PathMetadata sectionMeta = resolver.resolveForSection(section);
            // create config only for repeatable sections (REPEAT elementKind)
            if (Boolean.TRUE.equals(section.getRepeatable())) {
                TemplateElement repeatCfg = elementBuilder.buildTemplateElementFromRepeat(section, sectionMeta, dtv);
                out.add(repeatCfg);
            }
        }

        // produce fields
        Set<String> seen = new HashSet<>();
        // produce field configs
        for (FormDataElementConf f : snap.fields) {
            PathMetadata meta = resolver.resolveForField(f);
            if (!seen.add(meta.getIdPath())) {
                throw new IllegalStateException("Duplicate idPath detected: " + meta.getIdPath());
            }
            TemplateElement cfg = elementBuilder.buildTemplateElementFromField(f, meta, dtv);
            out.add(cfg);
        }

        // Persist (delete existing, bulk insert)
        final var ids = templateElementRepository.findIdsByTemplateUidAndTemplateVersionUid(templateUid, versionUid);
        templateElementRepository.deleteAllByIdInBatch(ids);
        templateElementRepository.persistAll(out);

        return out;
    }
}
