package org.nmcpye.datarun.jpa.datatemplate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.datatemplateelement.FormSectionConf;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
import org.nmcpye.datarun.jpa.dataelement.DataElement;
import org.nmcpye.datarun.jpa.dataelement.repository.DataElementRepository;
import org.nmcpye.datarun.jpa.datatemplate.ElementTemplateConfig;
import org.nmcpye.datarun.jpa.datatemplate.elementmapping.DataElementMeta;
import org.nmcpye.datarun.jpa.datatemplate.elementmapping.TemplateFieldMapper;
import org.nmcpye.datarun.jpa.datatemplate.exception.TemplateFieldValidationException;
import org.nmcpye.datarun.jpa.datatemplate.repository.ElementTemplateConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Generates element_template_configs rows from a DataTemplate's Version content and store them in one table.
 * Validates that category elements are allowed (reference types).
 *
 * @author Hamza Assada 19/08/2025 (7amza.it@gmail.com)
 * @see ElementTemplateConfig
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ElementTemplateGenerator {

    private final ElementTemplateConfigRepository elementTemplateConfigRepository;
    private final DataElementRepository dataElementRepository; // to resolve canonical element metadata
    private final TemplateFieldMapper templateFieldMapper;

    /// Primary generator taking parsed lists (test-friendly).
    @Transactional
    public List<ElementTemplateConfig> generateAndSaveFromParsed(
        String templateUid,
        String templateVersionUid,
        int templateVersionNo,
        List<FormDataElementConf> elements,
        List<FormSectionConf> sections) {

        Objects.requireNonNull(templateUid);
        Objects.requireNonNull(templateVersionUid);
        if (elements == null) elements = List.of();
        if (sections == null) sections = List.of();

        // Map element path/name -> FormDataElementConf for lookup by
        // section.category element id
        Map<String, FormDataElementConf> byUid = elements.stream()
            .collect(Collectors.toMap(
                ec -> ec.getId() != null ? ec.getId() : ec.getName(),
                Function.identity(),
                (a, b) -> a  // keep first if collision
            ));

        // Validate category elements referenced by sections
        List<String> validationErrors = new ArrayList<>();


        Map<String, String> sectionToCategoryElementUid = new HashMap<>();
        // repeatPath -> categoryElementId

        for (FormSectionConf s : sections) {
            String catElemUid = s.getCategoryDataElementId(); // or getRepeatCategoryElement depending on field name
            if (catElemUid == null) continue;
            // find the form element conf for that id or name
            FormDataElementConf catConf = byUid.get(catElemUid);
            if (catConf == null) {
                validationErrors.add("Section '" + s.getPath() + "' declares category element '" + catElemUid + "' which does not exist in template elements.");
                continue;
            }

            // resolve canonical DataElement meta
            Optional<DataElement> deOpt = dataElementRepository.findByUid(catConf.getId());
            if (deOpt.isEmpty()) {
                validationErrors.add("Category element '" + catElemUid + "' references unknown DataElement id '" + catConf.getId() + "'.");
                continue;
            }
            DataElement de = deOpt.get();
            ValueType vt = de.getValueType();

            if (!vt.isSystemReferenceType()) {
                validationErrors.add("Category element '" + catElemUid + "' has dataType '" + vt + "' which is not allowed as a repeat category (must be reference or select-one).");
                continue;
            }

            // OK — register mapping from repeat path to this category element uid
            sectionToCategoryElementUid.put(s.getPath(), catConf.getId());
        }

        if (!validationErrors.isEmpty()) {
            throw new TemplateFieldValidationException("Template version validation failed: " + String.join("; ", validationErrors));
        }

        // Build TemplateField list
        List<ElementTemplateConfig> rows = new ArrayList<>(elements.size());
        for (FormDataElementConf e : elements) {
            // find if element belongs to some repeat section (match section.path as prefix)
            String repeatPath = null;
            String categoryForRepeat = null;
            for (FormSectionConf s : sections) {
                if (s.getPath() != null && e.getPath() != null && e.getPath().startsWith(s.getPath())) {
                    if (Boolean.TRUE.equals(s.getRepeatable())) {
                        repeatPath = s.getPath();
                        // parent section repeat has category, that would categorize this element
                        if (sectionToCategoryElementUid.containsKey(s.getPath())) {
                            categoryForRepeat = sectionToCategoryElementUid.get(s.getPath());
                        }
                        break;
                    }
                }
            }

            // Resolve canonical data element metadata
            DataElementMeta meta;
            DataElement de = dataElementRepository.findByUid(e.getId())
                .orElseThrow(() -> new TemplateFieldValidationException("DataElement not found: " + e.getId()));
            boolean isRef = de.getValueType().isSystemReferenceType();
            String refTable = referenceTableFor(de.getValueType());
            meta = new DataElementMeta(de.getUid(), de.getValueType(), isRef, refTable);

            ElementTemplateConfig tf = templateFieldMapper.from(templateUid, templateVersionUid,
                templateVersionNo, e,
                meta, repeatPath, categoryForRepeat, sectionToCategoryElementUid
                    .containsValue(meta.elementUid()));
            tf.setDisplayLabel(e.getLabel());

            tf.setDefinitionJson(e);
            rows.add(tf);
        }

        // Persist (delete existing, bulk insert)
        final var ids = elementTemplateConfigRepository.findIdsByTemplateUidAndTemplateVersionUid(templateUid, templateVersionUid);
        elementTemplateConfigRepository.deleteAllByIdInBatch(ids);
        elementTemplateConfigRepository.persistAll(rows);

//        // Cache
//        cache.put(templateUid, templateVersionUid, rows);

        return rows;
    }

    private String referenceTableFor(ValueType vt) {
        if (vt == null) return null;
        return switch (vt) {
            case Activity -> "activity";
            case Team -> "team";
            case OrganisationUnit -> "org_unit";
            case Entity -> "entity_instance";
            case SelectOne, SelectMulti -> "option_value";
            default -> null;
        };
    }
}
