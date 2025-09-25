package org.nmcpye.datarun.jpa.analytics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.analytics.dto.Aggregation;
import org.nmcpye.datarun.jpa.analytics.domain.AnalyticsAttribute;
import org.nmcpye.datarun.jpa.analytics.domain.enums.AttributeScope;
import org.nmcpye.datarun.jpa.analytics.domain.enums.AttributeType;
import org.nmcpye.datarun.jpa.analytics.domain.enums.DataType;
import org.nmcpye.datarun.jpa.analytics.repository.AnalyticsAttributeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalyticsMetadataGenerator {

    private final AnalyticsAttributeRepository attributeRepository;
    // Assuming you have this repository for your existing domain model
    // private final ElementTemplateConfigRepository etcRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();


    @Transactional
    public void generateAndSaveAttributes(String templateVersionUid) {
        // Step 1: Clean slate for idempotency.
        attributeRepository.deleteAllByTemplateVersionUid(templateVersionUid);

        // Step 2: Generate attributes from template's structure (TO BE IMPLEMENTED LATER)
        // List<TemplateElement> configs = etcRepository.findByTemplateVersionUid(templateVersionUid);
        // for (TemplateElement etc : configs) {
        //     // Logic for dimensions, measures, and repeats will go here in the next steps.
        // }

        // Step 3: Generate "virtual" system attributes.
        // For this step, we will only implement the simplest one to prove the flow.
        List<AnalyticsAttribute> attributesToSave = new ArrayList<>(createSystemAttributes(templateVersionUid));

        // Step 4: Persist all generated attributes.
        attributeRepository.persistAll(attributesToSave);
    }

    // --- Private Helper Methods ---

    /**
     * Creates attributes that are inherent to the system, not defined in the template.
     * For now, we only create the "Submission Count" attribute.
     */
    private List<AnalyticsAttribute> createSystemAttributes(String templateVersionUid) {
        AnalyticsAttribute countAttr = new AnalyticsAttribute();

        countAttr.setUid("meas_submission_count");
        countAttr.setDisplayName(Map.of("en", "Submission Count"));
        countAttr.setAttributeType(AttributeType.MEASURE);
        countAttr.setDataType(DataType.NUMBER);
        countAttr.setAggregationType(Aggregation.COUNT);
        countAttr.setAttributeScope(AttributeScope.SUBMISSION); // It's a submission-level metric.
        countAttr.setTemplateVersionUid(templateVersionUid);

        // This mapping tells the future query engine to count the primary keys on the submission table.
//        countAttr.setDbMappingInfo(createJsonDbMapping("DATA_SUBMISSION", "uid", null));

        return List.of(countAttr);
    }

    // --- Utility Methods ---

    private String createJsonDisplayName(String name) {
        // For now, we default to English. This can be expanded for full i18n.
        return objectMapper.createObjectNode().put("en", name).toString();
    }

    private String createJsonDbMapping(String sourceTable, String sourceColumn, String elementUidFilter) {
        ObjectNode mapping = objectMapper.createObjectNode();
        mapping.put("source", sourceTable);
        mapping.put("column", sourceColumn);
        if (elementUidFilter != null) {
            mapping.put("element_uid_filter", elementUidFilter);
        }
        return mapping.toString();
    }
}
