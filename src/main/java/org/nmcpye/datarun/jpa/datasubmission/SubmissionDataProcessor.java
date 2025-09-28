package org.nmcpye.datarun.jpa.datasubmission;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.nmcpye.datarun.datatemplateelement.AbstractElement;
import org.nmcpye.datarun.datatemplateelement.FormSectionConf;
import org.nmcpye.datarun.jpa.datasubmission.service.FormDataProcessor;
import org.nmcpye.datarun.jpa.datasubmission.validation.CompositeSubmissionValidator;
import org.nmcpye.datarun.jpa.datasubmission.validation.DomainValidationException;
import org.nmcpye.datarun.jpa.datasubmission.validation.SubmissionAccessValidator;
import org.nmcpye.datarun.jpa.etl.model.TemplateElementMap;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Hamza Assada
 * @since 14/08/2025
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SubmissionDataProcessor {

    private final CompositeSubmissionValidator compositeValidator;
    private final SubmissionAccessValidator submissionAccessValidator;
    private final FormDataProcessor formDataProcessor;

    /**
     * '
     * -  run your composite validators first (form existence, assignment, permission).
     * <p>
     * - convert & validate repeat IDs (this will throw if any missing) (commented right now for migration to work)
     * <p>
     * - set canonical JSON back to submission
     *
     * @param submission incoming submission
     */
    public DataSubmission processIncomingSubmission(DataSubmission submission, CurrentUserDetails user) {
//        submissionAccessValidator.validateAccess(submission, user);
//        compositeValidator.validateAndEnrich(submission);
        final var enrichedFormData = formDataProcessor.enrichFormData(submission, true);
//         assertRepeatIdsPresent(submission.getFormData(),
//             elementMapService.getTemplateElementMap(submission.getForm(), submission.getFormVersion()));
        submission.setFormData(enrichedFormData);
        return submission;
    }


    // Validate repeat items have _id (JsonNode version)
    public void assertRepeatIdsPresent(JsonNode root, TemplateElementMap elementMap) {
        if (root == null || root.isNull()) return;
        // scan object fields recursively
        Deque<Pair<String, JsonNode>> stack = new ArrayDeque<>();
        stack.push(Pair.of("", root));

        while (!stack.isEmpty()) {
            Pair<String, JsonNode> cur = stack.pop();
            String path = cur.getLeft();
            JsonNode node = cur.getRight();
            if (node == null || node.isNull()) continue;
            if (!node.isObject()) continue;

            Iterator<Map.Entry<String, JsonNode>> it = node.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                String key = e.getKey();
                JsonNode value = e.getValue();
                String childPath = (path == null || path.isEmpty()) ? key : path + "." + key;

                AbstractElement element = elementMap.getElementByIdPathMap().get(childPath);
                if (element instanceof FormSectionConf section && Boolean.TRUE.equals(section.getRepeatable())) {
                    if (value == null || !value.isArray()) {
                        continue; // no instances to validate
                    }
                    ArrayNode arr = (ArrayNode) value;
                    for (int i = 0; i < arr.size(); i++) {
                        JsonNode item = arr.get(i);
                        if (!item.isObject()) {
                            throw new DomainValidationException("Repeat item at " + childPath + "[" + i + "] must be an object");
                        }
                        JsonNode idNode = item.get("_id");
                        if (idNode == null || idNode.isNull() || idNode.asText().isBlank()) {
                            throw new DomainValidationException("Missing repeat _id for " + childPath + "[" + i + "]");
                        }
                        // push nested fields within item for deeper repeats
                        stack.push(Pair.of(childPath, item));
                    }
                } else if (value != null && value.isObject()) {
                    stack.push(Pair.of(childPath, value));
                }
            }
        }
    }

    /**
     * Map -> JsonNode adapter.
     * to handle Map<String,Object> coming from older code / Mongo
     *
     * @param formData     either types
     * @param objectMapper mapper passed to method for testability porpuses
     * @return data as JsonNode
     */
    public JsonNode ensureJsonNode(Object formData, ObjectMapper objectMapper) {
        if (formData == null) {
            return objectMapper.createObjectNode();
        }
        if (formData instanceof JsonNode) {
            return (JsonNode) formData;
        }

        return objectMapper.convertValue(formData, JsonNode.class);
    }
}
