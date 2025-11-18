package org.nmcpye.datarun.etl.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.etl.dto.OutboxDto;
import org.nmcpye.datarun.etl.model.TallCanonicalRow;
import org.nmcpye.datarun.etl.service.TransformService;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
import org.nmcpye.datarun.jpa.datatemplate.TemplateElement;
import org.nmcpye.datarun.jpa.datatemplate.repository.TemplateElementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Pure mapping service: transforms an OutboxDto payload into a list of TallCanonicalRow DTOs.
 * <p>
 * Changes:
 * - Propagates parent repeat node metadata (_id/_parentId/_index/_submissionUid) from repeat objects
 * into child field rows so repeat fields receive correct repeat_instance_id and instance_key.
 * - Normalizes elementPath formatting to "repeat[0].field" style.
 * - Keeps updatedAt set at creation time (caller may overwrite).
 */
@Service
@RequiredArgsConstructor
public class TransformServiceImpl implements TransformService {

    private static final Logger log = LoggerFactory.getLogger(TransformServiceImpl.class);

    private static final int VALUE_TEXT_MAX = 4000;
    private static final int VALUE_JSON_MAX = 10000;

    private final TemplateElementRepository templateElementRepository;
    private final DataSubmissionRepository submissionRepository;
    private final ObjectMapper objectMapper;

    @Override
    public List<TallCanonicalRow> transform(OutboxDto outbox) {
        List<TallCanonicalRow> result = new ArrayList<>();
        if (outbox == null || outbox.getPayload() == null) return result;

        JsonNode root;
        try {
            root = objectMapper.readTree(outbox.getPayload());
        } catch (Exception e) {
            log.warn("Transform: invalid JSON payload for outboxId={} : {}", outbox.getOutboxId(), e.getMessage());
            return result;
        }

        // Determine templateVersionUid (payload.formVersion preferred, fallback to outbox.topic)
        String templateVersionUid = Optional.ofNullable(root.path("formVersion").asText(null))
            .orElse(Optional.ofNullable(outbox.getTopic()).orElse(null));
        if (templateVersionUid == null) {
            log.debug("Transform: missing templateVersionUid for outboxId={}, skipping", outbox.getOutboxId());
            return result;
        }

        List<TemplateElement> elements = templateElementRepository.findByTemplateVersionUid(templateVersionUid);
        if (elements == null || elements.isEmpty()) {
            log.debug("Transform: no template elements for version={}, outboxId={}", templateVersionUid, outbox.getOutboxId());
            return result;
        }

        // submission-level fallbacks
        String submissionId = outbox.getSubmissionId();
        String submissionUid = outbox.getSubmissionUid();
        Instant now = Instant.now();

        for (TemplateElement te : elements) {
            String jsonPath = te.getJsonDataPath();
            if (jsonPath == null || jsonPath.isBlank()) continue;

            List<PathHit> hits = resolvePathHits(root, jsonPath);
            if (te.isRepeat() && (hits == null || hits.isEmpty())) {
                log.debug("Transform: repeat TemplateElement {} has no array hits in payload for outboxId={}",
                    te.getCanonicalElementUid(), outbox.getOutboxId());
            }

            for (PathHit hit : hits) {
                JsonNode node = hit.node;
                if (node == null || node.isMissingNode() || node.isNull()) continue;

                // Prefer metadata from parentRepeatNode when present (repeat object holds _id/_parentId/_index)
                JsonNode parentRepeat = hit.parentRepeatNode;

                String repeatInstanceId = getText(parentRepeat, "_id");
                if (repeatInstanceId == null) repeatInstanceId = getText(node, "_id");

                String parentInstanceId = getText(parentRepeat, "_parentId");
                if (parentInstanceId == null) parentInstanceId = getText(node, "_parentId");

                Integer repeatIndex = null;
                if (parentRepeat != null && parentRepeat.has("_index") && parentRepeat.get("_index").canConvertToInt()) {
                    repeatIndex = parentRepeat.get("_index").intValue();
                } else if (node.has("_index") && node.get("_index").canConvertToInt()) {
                    repeatIndex = node.get("_index").intValue();
                } else if (hit.arrayIndex != null) {
                    repeatIndex = hit.arrayIndex;
                }

                String submissionUidForRow = Optional.ofNullable(getText(parentRepeat, "_submissionUid"))
                    .orElse(Optional.ofNullable(getText(node, "_submissionUid")).orElse(submissionUid));

                // Derive values
                String valueText = null;
                BigDecimal valueNumber = null;
                String valueJson = null;

                if (node.isNumber()) {
                    valueNumber = node.decimalValue();
                    valueText = node.asText();
                } else if (node.isTextual() || node.isBoolean() || node.isNull()) {
                    valueText = node.isNull() ? null : node.asText();
                } else if (node.isObject() || node.isArray()) {
                    String raw = node.toString();
                    if (raw.length() > VALUE_JSON_MAX) {
                        valueJson = raw.substring(0, VALUE_JSON_MAX) + "...(truncated)";
                        valueText = raw.substring(0, Math.min(VALUE_TEXT_MAX, raw.length())) + "...(truncated)";
                    } else {
                        valueJson = raw;
                    }
                } else {
                    valueText = node.asText(null);
                }

                if (valueText != null && valueText.length() > VALUE_TEXT_MAX) {
                    valueText = valueText.substring(0, VALUE_TEXT_MAX) + "...(truncated)";
                }

                boolean emptyText = (valueText == null || valueText.isEmpty());
                boolean emptyNumber = (valueNumber == null);
                boolean emptyJson = (valueJson == null || valueJson.isEmpty());
                if (emptyText && emptyNumber && emptyJson) continue;

                TallCanonicalRow row = TallCanonicalRow.builder()
                    .outboxId(outbox.getOutboxId())
                    .ingestId(null) // set by caller
                    .submissionId(submissionId)
                    .submissionUid(submissionUidForRow)
                    .submissionSerialNumber(outbox.getSubmissionSerialNumber())
                    .templateVersionUid(templateVersionUid)
                    .canonicalElementUid(te.getCanonicalElementUid())
                    .elementPath(hit.elementPath)
                    .repeatInstanceId(repeatInstanceId)
                    .parentInstanceId(parentInstanceId)
                    .repeatIndex(repeatIndex)
                    .valueText(valueText)
                    .valueNumber(valueNumber)
                    .valueJson(valueJson)
                    .createdAt(now)
                    .updatedAt(now)
                    .isDeleted(false)
                    .build();

                result.add(row);
            }
        }

        return result;
    }

    // --------------------
    // Helpers
    // --------------------

    private String getText(JsonNode node, String field) {
        if (node == null || node.isMissingNode()) return null;
        JsonNode v = node.get(field);
        if (v == null || v.isMissingNode() || v.isNull()) return null;
        return v.asText();
    }

    /**
     * Resolve dotted path into a list of PathHit.
     * - when encountering arrays, iterate elements and continue traversal for each.
     * - preserves the array element node as parentRepeatNode so leaf fields can inherit repeat metadata.
     * - elementPath normalized to "arr[0].field" style.
     * <p>
     * Important fix: when a child field is reached under an array element, do NOT append the array index again
     * to the child segment (avoid producing "arr[0].field[0]"). Only the array segment itself carries the index.
     */
    private List<PathHit> resolvePathHits(JsonNode root, String dottedPath) {
        List<PathHit> current = new ArrayList<>();
        current.add(new PathHit(root, null, "", null)); // start with no parent repeat

        String[] segments = dottedPath.split("\\.");
        for (String seg : segments) {
            List<PathHit> next = new ArrayList<>();
            for (PathHit ph : current) {
                JsonNode node = ph.node;
                if (node == null || node.isMissingNode() || node.isNull()) continue;

                JsonNode child = node.get(seg);
                if (child == null || child.isMissingNode() || child.isNull()) continue;

                if (child.isArray()) {
                    int size = child.size();
                    for (int i = 0; i < size; i++) {
                        JsonNode elem = child.get(i);
                        // array element path should include the index on the array segment
                        String newPath = buildPath(ph.elementPath, seg, i);
                        // If we're already under a repeat element, keep that node as parentRepeatNode;
                        // otherwise the element itself becomes the parentRepeatNode (for top-level arrays).
                        JsonNode parentRepeatForElem = ph.parentRepeatNode != null ? ph.parentRepeatNode : elem;
                        next.add(new PathHit(elem, i, newPath, parentRepeatForElem));

//                        // the element itself becomes the parentRepeatNode for downstream child fields
//                        // propagate parentRepeatNode if any (...)
//                        next.add(new PathHit(elem, i, newPath, elem));
                    }
                } else {
                    // child is a scalar/object under the current node.
                    // NOTE: do NOT re-apply array index here — if the parent was an array element,
                    // ph.elementPath already contains the "[i]" and we should not append index to child.
                    String newPath = buildPath(ph.elementPath, seg, -1);
                    next.add(new PathHit(child, ph.arrayIndex, newPath, ph.parentRepeatNode));
                }
            }
            current = next;
            if (current.isEmpty()) break;
        }
        return current;
    }

    /**
     * Build normalized element path like:
     * - "larvalHabitats[0]" for an array element
     * - "larvalHabitats[0].owner_name" for a child field
     * - "main.location_name" for a scalar path
     * <p>
     * arrayIndex == -1 means: do not append [index] for this segment.
     */
    private String buildPath(String base, String seg, int arrayIndex) {
        StringBuilder sb = new StringBuilder();
        if (base != null && !base.isEmpty()) {
            sb.append(base);
            sb.append(".");
        }
        sb.append(seg);
        if (arrayIndex >= 0) sb.append("[").append(arrayIndex).append("]");
        return sb.toString();
    }

    private static class PathHit {
        final JsonNode node;
        final Integer arrayIndex; // index if array traversed at last segment (may be null)
        final String elementPath;
        final JsonNode parentRepeatNode; // the array element node that acts as parent for leaf fields

        PathHit(JsonNode node, Integer arrayIndex, String elementPath, JsonNode parentRepeatNode) {
            this.node = node;
            this.arrayIndex = arrayIndex;
            this.elementPath = elementPath;
            this.parentRepeatNode = parentRepeatNode;
        }
    }
}

