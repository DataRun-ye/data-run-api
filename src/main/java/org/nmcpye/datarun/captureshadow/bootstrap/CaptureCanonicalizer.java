package org.nmcpye.datarun.captureshadow.bootstrap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.nmcpye.datarun.assignmentshadow.AssignmentShadowIdentities;
import org.nmcpye.datarun.captureshadow.CaptureShadowProtocol;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Component
final class CaptureCanonicalizer {

    private final ObjectMapper objectMapper;

    CaptureCanonicalizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    CanonicalCapture canonicalize(CaptureSourceRow source) {
        if (source.lastModifiedDate() == null) {
            throw new CaptureShadowBootstrapConflictException(
                "Submission " + source.uid() + " has no last_modified_date"
            );
        }
        if (source.orgUnitUid() == null || source.orgUnitUid().isBlank()) {
            throw new CaptureShadowBootstrapConflictException(
                "Submission " + source.uid() + " has no organization-unit UID"
            );
        }

        ObjectNode submission = objectMapper.createObjectNode();
        put(submission, "uid", source.uid());
        put(submission, "deleted", source.deleted());
        put(submission, "deletedAt", source.deletedAt());
        submission.set("formData", canonicalFormData(source));
        put(submission, "status", source.status());
        put(submission, "formUid", source.formUid());
        put(submission, "formVersionUid", source.formVersionUid());
        put(submission, "formVersionNumber", source.formVersionNumber());
        put(submission, "assignmentUid", source.assignmentUid());
        put(submission, "teamUid", source.teamUid());
        put(submission, "teamCode", source.teamCode());
        put(submission, "orgUnitUid", source.orgUnitUid());
        put(submission, "orgUnitCode", source.orgUnitCode());
        put(submission, "orgUnitName", source.orgUnitName());
        put(submission, "activityUid", source.activityUid());
        put(submission, "startEntryTime", source.startEntryTime());
        put(submission, "finishedEntryTime", source.finishedEntryTime());
        put(submission, "createdBy", source.createdBy());
        put(submission, "createdDate", source.createdDate());
        put(submission, "lastModifiedBy", source.lastModifiedBy());
        put(submission, "lastModifiedDate", source.lastModifiedDate());

        ObjectNode payload = objectMapper.createObjectNode();
        payload.set("submission", submission);
        return new CanonicalCapture(
            source,
            CaptureShadowProtocol.captureId(source.uid()),
            CaptureShadowProtocol.captureEventId(source.uid()),
            AssignmentShadowIdentities.orgUnitId(source.orgUnitUid()),
            source.lastModifiedDate(),
            payload
        );
    }

    void updateFingerprint(MessageDigest digest, CaptureSourceRow source) {
        CanonicalCapture capture = canonicalize(source);
        JsonNode formData = capture.payload().path("submission").path("formData");
        List<String> fields = Arrays.asList(
            source.submissionId(),
            source.uid(),
            Long.toString(source.serialNumber()),
            nullable(source.deleted()),
            format(source.deletedAt()),
            formData.isNull() ? null : formData.toString(),
            source.status(),
            source.formUid(),
            source.formVersionUid(),
            nullable(source.formVersionNumber()),
            source.assignmentUid(),
            source.teamUid(),
            source.teamCode(),
            source.orgUnitUid(),
            source.orgUnitCode(),
            source.orgUnitName(),
            source.activityUid(),
            format(source.startEntryTime()),
            format(source.finishedEntryTime()),
            source.createdBy(),
            format(source.createdDate()),
            source.lastModifiedBy(),
            format(source.lastModifiedDate())
        );
        if (fields.size() != CaptureShadowProtocol.SOURCE_FIELD_ORDER.size()) {
            throw new IllegalStateException("Capture fingerprint field contract drifted");
        }

        byte[][] encoded = new byte[fields.size()][];
        int rowLength = 0;
        for (int index = 0; index < fields.size(); index++) {
            String field = fields.get(index);
            if (field != null) {
                encoded[index] = field.getBytes(StandardCharsets.UTF_8);
                rowLength = Math.addExact(rowLength, encoded[index].length);
            }
            rowLength = Math.addExact(rowLength, Integer.BYTES);
        }
        digest.update(intBytes(rowLength));
        for (byte[] field : encoded) {
            digest.update(intBytes(field == null ? -1 : field.length));
            if (field != null) {
                digest.update(field);
            }
        }
    }

    private JsonNode canonicalFormData(CaptureSourceRow source) {
        if (source.formData() == null) {
            return NullNode.getInstance();
        }
        try {
            JsonNode formData = objectMapper.readTree(source.formData());
            if (formData == null || formData.isNull()) {
                throw invalidFormData(source, "JSON literal null");
            }
            if (!formData.isObject()) {
                throw invalidFormData(source, "non-object JSON");
            }
            return formData;
        } catch (JsonProcessingException exception) {
            throw new CaptureShadowBootstrapConflictException(
                "Submission " + source.uid() + " has invalid form_data JSON",
                exception
            );
        }
    }

    private CaptureShadowBootstrapConflictException invalidFormData(
        CaptureSourceRow source,
        String kind
    ) {
        return new CaptureShadowBootstrapConflictException(
            "Submission " + source.uid() + " has unsupported " + kind + " form_data"
        );
    }

    private static void put(ObjectNode node, String name, String value) {
        if (value == null) node.putNull(name); else node.put(name, value);
    }

    private static void put(ObjectNode node, String name, Boolean value) {
        if (value == null) node.putNull(name); else node.put(name, value);
    }

    private static void put(ObjectNode node, String name, Integer value) {
        if (value == null) node.putNull(name); else node.put(name, value);
    }

    private static void put(ObjectNode node, String name, Instant value) {
        put(node, name, format(value));
    }

    private static String nullable(Object value) {
        return value == null ? null : value.toString();
    }

    private static String format(Instant value) {
        return value == null ? null : DateTimeFormatter.ISO_INSTANT.format(value);
    }

    private static byte[] intBytes(int value) {
        return ByteBuffer.allocate(Integer.BYTES).putInt(value).array();
    }
}
