package org.nmcpye.datarun.jpa.datatemplategenerator;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.datatemplateelement.FormSectionConf;
import org.nmcpye.datarun.jpa.datatemplate.ElementTemplateConfig;
import org.nmcpye.datarun.jpa.datatemplate.TemplateVersion;
import org.nmcpye.datarun.jpa.datatemplate.repository.ElementTemplateConfigRepository;
import org.nmcpye.datarun.jpa.datatemplate.repository.TemplateVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Thin orchestrator that only composes small components. Pure generation — no database writes.
 * <p>
 * Usage:
 * List<ElementTemplateConfig> configs = generator.generate(templateUid, versionUid);
 * // persist configs using separate Publisher/Repository
 *
 * @author Hamza Assada
 * @since 09/09/2025
 */
@Service
@RequiredArgsConstructor
public class ElementTemplateConfigGeneratorService {
    private final TemplateVersionRepository versionRepository;
    private final FlatTemplateProcessor flatProcessor;
    private final ElementConfigBuilder elementConfigBuilder;
    private final ElementTemplateConfigRepository elementTemplateConfigRepository;


    @Transactional//(readOnly = true)
    public List<ElementTemplateConfig> generate(String templateUid, String versionUid) {
        Objects.requireNonNull(templateUid, "templateUid required");
        Objects.requireNonNull(versionUid, "versionUid required");

        TemplateVersion dtv = versionRepository.findByTemplateUidAndUid(templateUid, versionUid)
            .orElseThrow();
        FlatTemplateProcessor.TemplateFlatSnapshot snap = flatProcessor.process(dtv);

        // resolver needs sectionByName map
        MaterializedPathResolver resolver = new MaterializedPathResolver(snap.sectionByName);

        // produce repeat configs first (one per repeatable section)
        List<ElementTemplateConfig> out = new ArrayList<>(snap.fields.size() + snap.sectionByName.size());
        for (FormSectionConf section : snap.sectionByName.values()) {
            PathMetadata sectionMeta = resolver.resolveForSection(section);
            // create config only for repeatable sections (REPEAT elementKind)
            if (Boolean.TRUE.equals(section.getRepeatable())) {
                ElementTemplateConfig repeatCfg = elementConfigBuilder.buildRepeatConfigFromSection(section, sectionMeta, dtv);
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
            ElementTemplateConfig cfg = elementConfigBuilder.buildFieldConfigFromFormConf(f, meta, dtv);
            out.add(cfg);
        }

        // Persist (delete existing, bulk insert)
        final var ids = elementTemplateConfigRepository.findIdsByTemplateUidAndTemplateVersionUid(templateUid, versionUid);
        elementTemplateConfigRepository.deleteAllByIdInBatch(ids);
        elementTemplateConfigRepository.persistAll(out);

        return out;
    }
}
