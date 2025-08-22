package org.nmcpye.datarun.jpa.datatemplate.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.datatemplateelement.FormSectionConf;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
import org.nmcpye.datarun.jpa.dataelement.DataElement;
import org.nmcpye.datarun.jpa.dataelement.repository.DataElementRepository;
import org.nmcpye.datarun.jpa.datatemplate.ElementTemplate;
import org.nmcpye.datarun.jpa.datatemplate.elementmapping.DataElementMeta;
import org.nmcpye.datarun.jpa.datatemplate.elementmapping.TemplateFieldMapper;
import org.nmcpye.datarun.jpa.datatemplate.exception.TemplateFieldValidationException;
import org.nmcpye.datarun.jpa.datatemplate.repository.TemplateFieldJdbcRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates template_field rows from DataTemplateVersion content.
 * Validates that category elements are allowed (reference types).
 *
 * @author Hamza Assada 19/08/2025 (7amza.it@gmail.com)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateFieldGenerator {

    private final TemplateFieldJdbcRepository jdbcRepository;
    private final ObjectMapper objectMapper;
    private final DataElementRepository dataElementRepository; // to resolve canonical element metadata

    /**
     * Main entry point when you have JSON blobs (elementsJson and sectionsJson are JSON arrays).
     */
    @Transactional
    public List<ElementTemplate> generateAndSaveFromJson(
        String templateId,
        String versionId,
        int versionNo,
        List<FormDataElementConf> elements,
        List<FormSectionConf> sections) throws Exception {

        return generateAndSaveFromParsed(templateId, versionId, versionNo, elements, sections);
    }

    /**
     * Primary generator taking parsed lists (test-friendly).
     */
    @Transactional
    public List<ElementTemplate> generateAndSaveFromParsed(
        String templateId,
        String versionId,
        int versionNo,
        List<FormDataElementConf> elements,
        List<FormSectionConf> sections) {

        Objects.requireNonNull(templateId);
        Objects.requireNonNull(versionId);
        if (elements == null) elements = List.of();
        if (sections == null) sections = List.of();

        // Map element path/name -> FormDataElementConf for lookup by
        // section.category element id
        Map<String, FormDataElementConf> byId = elements.stream()
            .collect(Collectors.toMap(
                e -> e.getId() != null ? e.getId() : e.getName(),
                e -> e,
                (a, b) -> a  // keep first if collision
            ));

        // Validate category elements referenced by sections
        List<String> validationErrors = new ArrayList<>();


        Map<String, String> sectionToCategoryElementId = new HashMap<>();
        // repeatPath -> categoryElementId

        for (FormSectionConf s : sections) {
            String catElemId = s.getCategoryDataElementId(); // or getRepeatCategoryElement depending on field name
            if (catElemId == null) continue;
            // find the form element conf for that id or name
            FormDataElementConf catConf = byId.get(catElemId);
            if (catConf == null) {
                validationErrors.add("Section '" + s.getPath() + "' declares category element '" + catElemId + "' which does not exist in template elements.");
                continue;
            }

            // resolve canonical DataElement meta
            Optional<DataElement> deOpt = dataElementRepository.findById(catConf.getId());
            if (deOpt.isEmpty()) {
                validationErrors.add("Category element '" + catElemId + "' references unknown DataElement id '" + catConf.getId() + "'.");
                continue;
            }
            DataElement de = deOpt.get();
            ValueType vt = de.getType();

            if (!vt.isCategoricalType()) {
                validationErrors.add("Category element '" + catElemId + "' has valueType '" + vt + "' which is not allowed as a repeat category (must be reference or select-one).");
                continue;
            }

            // OK — register mapping from repeat path to this category element id
            sectionToCategoryElementId.put(s.getPath(), catConf.getId());
        }

        if (!validationErrors.isEmpty()) {
            throw new TemplateFieldValidationException("Template version validation failed: " + String.join("; ", validationErrors));
        }

        // Build TemplateField list
        List<ElementTemplate> rows = new ArrayList<>(elements.size());
        for (FormDataElementConf e : elements) {
            // find if element belongs to some repeat section (match section.path as prefix)
            String repeatPath = null;
            String categoryForRepeat = null;
            for (FormSectionConf s : sections) {
                if (s.getPath() != null && e.getPath() != null && e.getPath().startsWith(s.getPath())) {
                    if (Boolean.TRUE.equals(s.getRepeatable())) {
                        repeatPath = s.getPath();
                        if (sectionToCategoryElementId.containsKey(s.getPath())) {
                            categoryForRepeat = sectionToCategoryElementId.get(s.getPath());
                        }
                        break;
                    }
                }
            }

            // Resolve canonical data element metadata
            DataElementMeta meta;
            DataElement de = dataElementRepository.findById(e.getId())
                .orElseThrow(() -> new TemplateFieldValidationException("DataElement not found: " + e.getId()));
            boolean isRef = de.getType().isCategoricalType();
            String refTable = referenceTableFor(de.getType());
            meta = new DataElementMeta(de.getId(), de.getType(), isRef, refTable);

            ElementTemplate tf = TemplateFieldMapper.from(templateId, versionId, versionNo, e,
                meta, repeatPath, categoryForRepeat);

            // serialize display label and definition JSON using objectMapper
            try {
                tf.setDisplayLabelJson(objectMapper.writeValueAsString(e.getLabel()));
            } catch (Exception ex) {
                log.warn("could not serialize element label into json string for template field table: {}, {}",
                    e.getName(), e.getLabel());
                tf.setDisplayLabelJson(null);
            }
            try {
                tf.setDefinitionJson(objectMapper.writeValueAsString(e));
                log.warn("could not serialize element definition into json string for template field table: {}",
                    e.getName());
            } catch (Exception ex) {
                tf.setDefinitionJson(null);
            }

            rows.add(tf);
        }

        // Persist (delete existing, bulk insert)
        jdbcRepository.deleteByTemplateAndVersion(templateId, versionId);
        jdbcRepository.bulkInsert(rows);

//        // Cache
//        cache.put(templateId, versionId, rows);

        return rows;
    }

    private String referenceTableFor(ValueType vt) {
        if (vt == null) return null;
        return switch (vt) {
            case Team -> "team";
            case OrganisationUnit -> "org_unit";
            case Activity -> "activity";
            case Entity -> "entity_instance";
            case SelectOne -> "option";
            default -> null;
        };
    }
}
