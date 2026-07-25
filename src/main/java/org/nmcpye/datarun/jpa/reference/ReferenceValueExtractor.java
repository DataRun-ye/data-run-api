package org.nmcpye.datarun.jpa.reference;

import com.fasterxml.jackson.databind.JsonNode;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
import org.nmcpye.datarun.jpa.datasubmission.validation.DomainValidationException;
import org.nmcpye.datarun.jpa.datatemplate.dto.DataTemplateInstanceDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Component
public class ReferenceValueExtractor {

    public List<ReferenceValueOccurrence> extract(
        String submissionUid,
        DataTemplateInstanceDto template,
        JsonNode formData) {
        if (formData == null || formData.isNull() || template.getFields() == null) {
            return List.of();
        }

        List<ReferenceValueOccurrence> values = new ArrayList<>();
        template.getFields().stream()
            .filter(Objects::nonNull)
            .filter(field -> ValueType.Reference.equals(field.getType()))
            .forEach(field -> collectFieldValues(
                submissionUid,
                field,
                formData,
                values));
        return List.copyOf(values);
    }

    private void collectFieldValues(
        String submissionUid,
        FormDataElementConf field,
        JsonNode formData,
        List<ReferenceValueOccurrence> values) {
        String elementPath = field.getPath() == null || field.getPath().isBlank()
            ? field.getName()
            : field.getPath();
        List<String> jsonPath = new ArrayList<>(
            Arrays.asList(elementPath.split("\\.")));
        jsonPath.set(jsonPath.size() - 1, field.getName());
        collectAtPath(
            submissionUid,
            elementPath,
            formData,
            jsonPath,
            0,
            values);
    }

    private void collectAtPath(
        String submissionUid,
        String elementPath,
        JsonNode node,
        List<String> jsonPath,
        int segmentIndex,
        List<ReferenceValueOccurrence> values) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return;
        }
        if (node.isArray() && segmentIndex < jsonPath.size()) {
            node.forEach(item -> collectAtPath(
                submissionUid,
                elementPath,
                item,
                jsonPath,
                segmentIndex,
                values));
            return;
        }
        if (segmentIndex < jsonPath.size()) {
            if (!node.isObject()) {
                throw invalidValue(submissionUid, elementPath);
            }
            collectAtPath(
                submissionUid,
                elementPath,
                node.get(jsonPath.get(segmentIndex)),
                jsonPath,
                segmentIndex + 1,
                values);
            return;
        }
        if (!node.isTextual()) {
            throw invalidValue(submissionUid, elementPath);
        }

        String uid = node.textValue();
        if (uid == null || uid.isBlank()) {
            return;
        }
        values.add(new ReferenceValueOccurrence(elementPath, uid));
    }

    private DomainValidationException invalidValue(
        String submissionUid,
        String elementPath) {
        return new DomainValidationException(
            ErrorCode.E4117,
            submissionUid,
            elementPath);
    }
}
