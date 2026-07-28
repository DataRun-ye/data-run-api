package org.nmcpye.datarun.captureshadow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

@Component
public class CaptureSubmissionCanonicalizer {

    private final ObjectMapper objectMapper;

    public CaptureSubmissionCanonicalizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ObjectNode canonicalize(DataSubmission submission) {
        return canonicalize(new CanonicalSubmissionSnapshot(
            submission.getUid(),
            submission.getDeleted(),
            submission.getDeletedAt(),
            submission.getFormData(),
            submission.getStatus() == null
                ? null
                : submission.getStatus().name(),
            submission.getForm(),
            submission.getFormVersion(),
            submission.getVersion(),
            submission.getAssignment(),
            submission.getTeam(),
            submission.getTeamCode(),
            submission.getOrgUnit(),
            submission.getOrgUnitCode(),
            submission.getOrgUnitName(),
            submission.getActivity(),
            submission.getStartEntryTime(),
            submission.getFinishedEntryTime(),
            submission.getCreatedBy(),
            submission.getCreatedDate(),
            submission.getLastModifiedBy(),
            submission.getLastModifiedDate()
        ));
    }

    public ObjectNode canonicalize(CanonicalSubmissionSnapshot source) {
        ObjectNode submission = objectMapper.createObjectNode();
        put(submission, "uid", source.uid());
        put(submission, "deleted", source.deleted());
        put(submission, "deletedAt", source.deletedAt());
        JsonNode formData = source.formData();
        submission.set(
            "formData",
            formData == null ? NullNode.getInstance() : formData.deepCopy()
        );
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
        return submission;
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
        put(
            node,
            name,
            value == null
                ? null
                : DateTimeFormatter.ISO_INSTANT.format(
                    roundToPostgresMicros(value)
                )
        );
    }

    private static Instant roundToPostgresMicros(Instant value) {
        long roundedNanos =
            ((long) value.getNano() + 500L) / 1_000L * 1_000L;
        if (roundedNanos == 1_000_000_000L) {
            return Instant.ofEpochSecond(value.getEpochSecond() + 1L);
        }
        return Instant.ofEpochSecond(
            value.getEpochSecond(),
            roundedNanos
        );
    }
}
