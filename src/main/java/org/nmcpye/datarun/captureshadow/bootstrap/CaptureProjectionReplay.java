package org.nmcpye.datarun.captureshadow.bootstrap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.nmcpye.datarun.assignmentshadow.TransitionIdentityResolver;
import org.nmcpye.datarun.captureshadow.CaptureAcceptanceValidator;
import org.nmcpye.datarun.captureshadow.CaptureAcceptanceValidator.AcceptanceCheck;
import org.nmcpye.datarun.captureshadow.CaptureCurrentProjectionPort;
import org.nmcpye.datarun.captureshadow.CaptureIdentityLink;
import org.nmcpye.datarun.captureshadow.CaptureShadowProtocol;
import org.nmcpye.datarun.eventjournal.JournalEvent;
import org.nmcpye.datarun.transition.TransitionCheckpointStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CaptureProjectionReplay {

    static final int PAGE_SIZE = 250;

    private static final Set<String> SUBMISSION_FIELDS = Set.of(
        "uid",
        "deleted",
        "deletedAt",
        "formData",
        "status",
        "formUid",
        "formVersionUid",
        "formVersionNumber",
        "assignmentUid",
        "teamUid",
        "teamCode",
        "orgUnitUid",
        "orgUnitCode",
        "orgUnitName",
        "activityUid",
        "startEntryTime",
        "finishedEntryTime",
        "createdBy",
        "createdDate",
        "lastModifiedBy",
        "lastModifiedDate"
    );

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate namedJdbc;
    private final ObjectMapper objectMapper;
    private final CaptureSourceReader sourceReader;
    private final CaptureCanonicalizer canonicalizer;
    private final CaptureCurrentProjectionPort currentProjection;
    private final CaptureAcceptanceValidator acceptanceValidator;
    private final TransitionCheckpointStore checkpoints;

    public CaptureProjectionReplay(
        JdbcTemplate jdbc,
        NamedParameterJdbcTemplate namedJdbc,
        ObjectMapper objectMapper,
        CaptureSourceReader sourceReader,
        CaptureCanonicalizer canonicalizer,
        CaptureCurrentProjectionPort currentProjection,
        CaptureAcceptanceValidator acceptanceValidator,
        TransitionCheckpointStore checkpoints
    ) {
        this.jdbc = jdbc;
        this.namedJdbc = namedJdbc;
        this.objectMapper = objectMapper;
        this.sourceReader = sourceReader;
        this.canonicalizer = canonicalizer;
        this.currentProjection = currentProjection;
        this.acceptanceValidator = acceptanceValidator;
        this.checkpoints = checkpoints;
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public CaptureReplayReport replay(CaptureReplayMode mode) {
        Objects.requireNonNull(mode);
        long sourceCount = sourceReader.count();
        Long maximumSerial = sourceReader.maximumSerial();
        BootstrapBoundary bootstrapBoundary = bootstrapBoundary();
        long afterSerial = CaptureShadowBootstrap.INITIAL_SERIAL_CURSOR;
        long captures = 0;
        long events = 0;
        long pointersInserted = 0;

        while (true) {
            List<CaptureSourceRow> rows = sourceReader.readPage(
                afterSerial,
                maximumSerial,
                PAGE_SIZE
            );
            if (rows.isEmpty()) {
                break;
            }
            ReplayPageResult page = replayPage(
                rows,
                bootstrapBoundary,
                mode
            );
            captures += rows.size();
            events += page.eventCount();
            pointersInserted += page.pointersInserted();
            afterSerial = rows.get(rows.size() - 1).serialNumber();
        }
        if (captures != sourceCount) {
            throw conflict(
                "Capture replay source count changed expected="
                    + sourceCount + " actual=" + captures
            );
        }
        requireCount("capture_identity_link", sourceCount);
        requireCount("capture_current_projection", sourceCount);
        long scannedEvents = scanEveryCaptureEvent();
        if (scannedEvents != events) {
            throw conflict(
                "Capture event ownership count differs owned="
                    + events + " scanned=" + scannedEvents
            );
        }
        return new CaptureReplayReport(
            captures,
            events,
            pointersInserted
        );
    }

    private ReplayPageResult replayPage(
        List<CaptureSourceRow> rows,
        BootstrapBoundary bootstrapBoundary,
        CaptureReplayMode mode
    ) {
        Map<UUID, ExpectedCapture> expected = new LinkedHashMap<>();
        for (CaptureSourceRow row : rows) {
            CanonicalCapture capture = canonicalizer.canonicalize(row);
            expected.put(
                capture.captureId(),
                new ExpectedCapture(row, capture)
            );
        }

        requireExactIdentities(expected.values());
        Map<UUID, UUID> pointers = currentPointers(expected.keySet());
        Map<UUID, List<ReplayEvent>> eventsByCapture =
            eventsByCapture(expected);
        PageRequirements requirements = new PageRequirements();
        for (Map.Entry<UUID, List<ReplayEvent>> entry
            : eventsByCapture.entrySet()) {
            List<ReplayEvent> validated = entry.getValue().stream()
                .map(event -> validateEvent(
                    entry.getKey(),
                    event.event(),
                    requirements
                ))
                .toList();
            entry.setValue(validated);
        }
        requireExactOrgUnitAliases(requirements.orgUnitAliases);
        acceptanceValidator.validateAll(requirements.acceptances);

        List<PointerInsert> missingPointers = new ArrayList<>();
        long eventCount = 0;
        for (ExpectedCapture value : expected.values()) {
            List<ReplayEvent> captureEvents = eventsByCapture.getOrDefault(
                value.capture().captureId(),
                List.of()
            );
            if (captureEvents.isEmpty()) {
                throw conflict(
                    "Capture has no immutable facts: "
                        + value.capture().captureId()
                );
            }
            requireBootstrapBoundary(
                value,
                captureEvents,
                bootstrapBoundary
            );
            ReplayEvent head = deriveHead(value.capture(), captureEvents);
            JsonNode currentSubmission =
                value.capture().payload().path("submission");
            if (!head.submission().equals(currentSubmission)) {
                throw conflict(
                    "Capture current head differs from data_submission for "
                        + value.capture().captureId()
                        + " fields="
                        + differingFields(
                            head.submission(),
                            currentSubmission
                        )
                );
            }
            UUID pointer = pointers.get(value.capture().captureId());
            if (pointer == null) {
                if (mode == CaptureReplayMode.VALIDATE) {
                    throw conflict(
                        "Capture current pointer is missing for "
                            + value.capture().captureId()
                    );
                }
                missingPointers.add(new PointerInsert(
                    value.capture().captureId(),
                    head.event().eventId()
                ));
            } else if (!pointer.equals(head.event().eventId())) {
                throw conflict(
                    "Capture current pointer differs from derived head for "
                        + value.capture().captureId()
                );
            }
            eventCount += captureEvents.size();
        }

        for (PointerInsert pointer : missingPointers) {
            currentProjection.insertInitialPointer(
                pointer.captureId(),
                pointer.sourceEventId()
            );
        }
        return new ReplayPageResult(
            eventCount,
            missingPointers.size()
        );
    }

    private void requireExactIdentities(
        java.util.Collection<ExpectedCapture> captures
    ) {
        Set<UUID> captureIds = captures.stream()
            .map(value -> value.capture().captureId())
            .collect(Collectors.toSet());
        Set<String> submissionUids = captures.stream()
            .map(value -> value.row().uid())
            .collect(Collectors.toSet());
        Set<String> submissionIds = captures.stream()
            .map(value -> value.row().submissionId())
            .collect(Collectors.toSet());
        Set<Long> serialNumbers = captures.stream()
            .map(value -> value.row().serialNumber())
            .collect(Collectors.toSet());
        List<CaptureIdentityLink> candidates = namedJdbc.query(
            """
                SELECT capture_id, baseline_submission_uid,
                       baseline_submission_id, baseline_serial_number
                FROM capture_identity_link
                WHERE capture_id IN (:captureIds)
                   OR baseline_submission_uid IN (:submissionUids)
                   OR baseline_submission_id IN (:submissionIds)
                   OR baseline_serial_number IN (:serialNumbers)
                """,
            new MapSqlParameterSource()
                .addValue("captureIds", captureIds)
                .addValue("submissionUids", submissionUids)
                .addValue("submissionIds", submissionIds)
                .addValue("serialNumbers", serialNumbers),
            (resultSet, rowNumber) -> new CaptureIdentityLink(
                resultSet.getObject("capture_id", UUID.class),
                resultSet.getString("baseline_submission_uid"),
                resultSet.getString("baseline_submission_id"),
                resultSet.getLong("baseline_serial_number")
            )
        );
        for (ExpectedCapture value : captures) {
            CaptureIdentityLink expected = new CaptureIdentityLink(
                value.capture().captureId(),
                value.row().uid(),
                value.row().submissionId(),
                value.row().serialNumber()
            );
            List<CaptureIdentityLink> matching = candidates.stream()
                .filter(candidate ->
                    candidate.captureId().equals(expected.captureId())
                        || candidate.baselineSubmissionUid().equals(
                            expected.baselineSubmissionUid()
                        )
                        || candidate.baselineSubmissionId().equals(
                            expected.baselineSubmissionId()
                        )
                        || candidate.baselineSerialNumber()
                        == expected.baselineSerialNumber()
                )
                .toList();
            if (matching.size() != 1
                || !matching.get(0).equals(expected)) {
                throw conflict(
                    "Conflicting capture identity for submission "
                        + expected.baselineSubmissionUid()
                );
            }
        }
    }

    private Map<UUID, UUID> currentPointers(Set<UUID> captureIds) {
        return uniqueUuidMap(
            namedJdbc.query(
                """
                    SELECT capture_id, source_event_id
                    FROM capture_current_projection
                    WHERE capture_id IN (:captureIds)
                    """,
                new MapSqlParameterSource("captureIds", captureIds),
                (resultSet, rowNumber) -> Map.entry(
                    resultSet.getObject("capture_id", UUID.class),
                    resultSet.getObject("source_event_id", UUID.class)
                )
            ),
            "capture current pointers"
        );
    }

    private Map<UUID, List<ReplayEvent>> eventsByCapture(
        Map<UUID, ExpectedCapture> expected
    ) {
        Map<UUID, UUID> bootstrapOwners = expected.values().stream()
            .collect(Collectors.toMap(
                value -> value.capture().eventId(),
                value -> value.capture().captureId()
            ));
        Set<String> captureIdValues = expected.keySet().stream()
            .map(UUID::toString)
            .collect(Collectors.toSet());
        Map<UUID, List<ReplayEvent>> eventsByCapture =
            new LinkedHashMap<>();
        long afterPosition = 0;
        while (true) {
            List<JournalEvent> events = namedJdbc.query(
                """
                    SELECT
                        journal_position,
                        event_id,
                        event_type,
                        shape_ref,
                        activity_ref,
                        subject_type,
                        subject_id,
                        actor_id,
                        recorded_at,
                        payload::text AS payload
                    FROM event_journal
                    WHERE event_type = :eventType
                      AND journal_position > :afterPosition
                      AND (
                          (
                              shape_ref = :bootstrapShape
                              AND event_id IN (:bootstrapEventIds)
                          )
                          OR (
                              shape_ref = :liveShape
                              AND payload ->> 'captureId'
                                  IN (:captureIds)
                          )
                      )
                    ORDER BY journal_position
                    LIMIT :limit
                    """,
                new MapSqlParameterSource()
                    .addValue(
                        "eventType",
                        CaptureShadowProtocol.CAPTURE_EVENT_TYPE
                    )
                    .addValue(
                        "bootstrapShape",
                        CaptureShadowProtocol.CAPTURE_SHAPE_REF
                    )
                    .addValue(
                        "bootstrapEventIds",
                        bootstrapOwners.keySet()
                    )
                    .addValue(
                        "liveShape",
                        CaptureShadowProtocol.LIVE_CAPTURE_SHAPE_REF
                    )
                    .addValue("captureIds", captureIdValues)
                    .addValue("afterPosition", afterPosition)
                    .addValue("limit", PAGE_SIZE),
                (resultSet, rowNumber) -> mapEvent(resultSet)
            );
            if (events.isEmpty()) {
                return eventsByCapture;
            }
            for (JournalEvent event : events) {
                UUID captureId;
                if (CaptureShadowProtocol.CAPTURE_SHAPE_REF.equals(
                    event.shapeRef()
                )) {
                    captureId = bootstrapOwners.get(event.eventId());
                } else {
                    captureId = parseUuid(
                        event.payload().path("captureId").textValue(),
                        "live capture ID"
                    );
                }
                if (captureId == null || !expected.containsKey(captureId)) {
                    throw conflict(
                        "Capture event has no owner in replay page: "
                            + event.eventId()
                    );
                }
                eventsByCapture.computeIfAbsent(
                    captureId,
                    ignored -> new ArrayList<>()
                ).add(new ReplayEvent(event, null, null, false));
            }
            afterPosition = events.get(events.size() - 1)
                .journalPosition();
        }
    }

    private ReplayEvent validateEvent(
        UUID captureId,
        JournalEvent event,
        PageRequirements requirements
    ) {
        if (!CaptureShadowProtocol.CAPTURE_EVENT_TYPE.equals(
            event.eventType()
        ) || !CaptureShadowProtocol.CAPTURE_SUBJECT_TYPE.equals(
            event.subjectType()
        )) {
            throw conflict(
                "Capture event envelope is invalid: " + event.eventId()
            );
        }
        if (CaptureShadowProtocol.CAPTURE_SHAPE_REF.equals(event.shapeRef())) {
            return validateBootstrapEvent(
                captureId,
                event,
                requirements
            );
        }
        if (CaptureShadowProtocol.LIVE_CAPTURE_SHAPE_REF.equals(
            event.shapeRef()
        )) {
            return validateLiveEvent(captureId, event, requirements);
        }
        throw conflict("Unknown capture shape: " + event.shapeRef());
    }

    private ReplayEvent validateBootstrapEvent(
        UUID captureId,
        JournalEvent event,
        PageRequirements requirements
    ) {
        JsonNode payload = event.payload();
        if (!payload.isObject()
            || payload.size() != 1
            || !payload.path("submission").isObject()
            || !CaptureShadowProtocol.SYSTEM_ACTOR.equals(event.actorId())) {
            throw conflict(
                "Bootstrap capture fact is malformed: " + event.eventId()
            );
        }
        JsonNode submission = payload.path("submission");
        validateCanonicalSubmission(submission);
        String uid = submission.path("uid").textValue();
        if (!CaptureShadowProtocol.captureId(uid).equals(captureId)
            || !CaptureShadowProtocol.captureEventId(uid)
            .equals(event.eventId())) {
            throw conflict(
                "Bootstrap capture fact identity is invalid: "
                    + event.eventId()
            );
        }
        collectEnvelopeRequirements(event, submission, requirements);
        String lastModified = submission.path("lastModifiedDate").textValue();
        if (lastModified == null
            || !event.recordedAt().equals(parseInstant(
                lastModified,
                "bootstrap lastModifiedDate"
            ))) {
            throw conflict(
                "Bootstrap capture recorded_at is not canonical: "
                    + event.eventId()
            );
        }
        return new ReplayEvent(event, null, submission, true);
    }

    private ReplayEvent validateLiveEvent(
        UUID captureId,
        JournalEvent event,
        PageRequirements requirements
    ) {
        JsonNode payload = event.payload();
        if (!payload.isObject()
            || payload.size() != 4
            || !payload.path("captureId").isTextual()
            || !captureId.toString().equals(
                payload.path("captureId").textValue()
            )
            || !payload.has("previousEventId")
            || !payload.path("submission").isObject()) {
            throw conflict(
                "Live capture fact is malformed: " + event.eventId()
            );
        }
        JsonNode submission = payload.path("submission");
        validateCanonicalSubmission(submission);
        if (!captureId.equals(CaptureShadowProtocol.captureId(
            submission.path("uid").textValue()
        ))) {
            throw conflict(
                "Live capture payload UID does not match capture: "
                    + event.eventId()
            );
        }
        collectEnvelopeRequirements(event, submission, requirements);
        UUID actorId = parseUuid(event.actorId(), "capture actor");
        requirements.acceptances.add(new AcceptanceCheck(
            payload.path("acceptance"),
            actorId,
            submission.path("assignmentUid").textValue(),
            false
        ));
        JsonNode previous = payload.path("previousEventId");
        UUID previousEventId = null;
        if (!previous.isNull()) {
            if (!previous.isTextual()) {
                throw conflict(
                    "Live capture predecessor is malformed: "
                        + event.eventId()
                );
            }
            previousEventId = parseUuid(
                previous.textValue(),
                "capture predecessor"
            );
        }
        return new ReplayEvent(
            event,
            previousEventId,
            submission,
            false
        );
    }

    private void collectEnvelopeRequirements(
        JournalEvent event,
        JsonNode submission,
        PageRequirements requirements
    ) {
        String activityUid = nullableText(
            submission.path("activityUid"),
            "activityUid"
        );
        String orgUnitUid = nullableText(
            submission.path("orgUnitUid"),
            "orgUnitUid"
        );
        if (orgUnitUid == null
            || !Objects.equals(activityUid, event.activityRef())) {
            throw conflict(
                "Capture event activity or organization unit is invalid: "
                    + event.eventId()
            );
        }
        UUID expectedOrgUnitId =
            TransitionIdentityResolver.orgUnitIdFor(orgUnitUid);
        if (!expectedOrgUnitId.equals(event.subjectId())) {
            throw conflict(
                "Capture event subject is not the canonical organization unit: "
                    + event.eventId()
            );
        }
        requirements.orgUnitAliases.add(
            new OrgUnitAlias(expectedOrgUnitId, orgUnitUid)
        );
    }

    private void requireExactOrgUnitAliases(
        Set<OrgUnitAlias> expected
    ) {
        if (expected.isEmpty()) {
            return;
        }
        Set<UUID> ids = expected.stream()
            .map(OrgUnitAlias::orgUnitId)
            .collect(Collectors.toSet());
        Set<String> uids = expected.stream()
            .map(OrgUnitAlias::baselineOrgUnitUid)
            .collect(Collectors.toSet());
        List<OrgUnitAlias> candidates = namedJdbc.query(
            """
                SELECT org_unit_id, baseline_org_unit_uid
                FROM org_unit_identity_link
                WHERE org_unit_id IN (:ids)
                   OR baseline_org_unit_uid IN (:uids)
                """,
            new MapSqlParameterSource()
                .addValue("ids", ids)
                .addValue("uids", uids),
            (resultSet, rowNumber) -> new OrgUnitAlias(
                resultSet.getObject("org_unit_id", UUID.class),
                resultSet.getString("baseline_org_unit_uid")
            )
        );
        for (OrgUnitAlias alias : expected) {
            List<OrgUnitAlias> matching = candidates.stream()
                .filter(candidate ->
                    candidate.orgUnitId().equals(alias.orgUnitId())
                        || candidate.baselineOrgUnitUid().equals(
                            alias.baselineOrgUnitUid()
                        )
                )
                .toList();
            if (matching.size() != 1
                || !matching.get(0).equals(alias)) {
                throw conflict(
                    "Capture event organization-unit alias is not exact: "
                        + alias.baselineOrgUnitUid()
                );
            }
        }
    }

    private void requireBootstrapBoundary(
        ExpectedCapture capture,
        List<ReplayEvent> events,
        BootstrapBoundary boundary
    ) {
        long bootstrapEvents = events.stream()
            .filter(ReplayEvent::bootstrap)
            .count();
        if (!boundary.present()) {
            if (bootstrapEvents != 0) {
                throw conflict(
                    "Capture bootstrap fact count is invalid for "
                        + capture.capture().captureId()
                );
            }
            return;
        }
        boolean insideBoundary = boundary.maximumSerial() != null
            && capture.row().serialNumber() <= boundary.maximumSerial();
        long expectedBootstrapEvents = insideBoundary ? 1 : 0;
        if (bootstrapEvents != expectedBootstrapEvents) {
            throw conflict(
                "Capture bootstrap fact count is invalid for "
                    + capture.capture().captureId()
            );
        }
    }

    private ReplayEvent deriveHead(
        CanonicalCapture capture,
        List<ReplayEvent> events
    ) {
        Map<UUID, ReplayEvent> byId = new LinkedHashMap<>();
        Map<UUID, ReplayEvent> successorByPredecessor =
            new HashMap<>();
        List<ReplayEvent> roots = new ArrayList<>();
        for (ReplayEvent event : events) {
            if (byId.put(event.event().eventId(), event) != null) {
                throw conflict(
                    "Duplicate capture event ID: "
                        + event.event().eventId()
                );
            }
        }
        for (ReplayEvent event : events) {
            UUID predecessor = event.previousEventId();
            if (predecessor == null) {
                roots.add(event);
                continue;
            }
            if (!byId.containsKey(predecessor)) {
                throw conflict(
                    "Missing or cross-capture predecessor "
                        + predecessor + " for " + capture.captureId()
                );
            }
            if (byId.get(predecessor).event().journalPosition()
                >= event.event().journalPosition()) {
                throw conflict(
                    "Capture predecessor is not earlier than successor "
                        + event.event().eventId()
                );
            }
            if (successorByPredecessor.put(predecessor, event) != null) {
                throw conflict(
                    "Capture chain forks at " + predecessor
                );
            }
        }
        if (roots.size() != 1) {
            throw conflict(
                "Capture chain must have one root: " + capture.captureId()
            );
        }
        if (events.stream().anyMatch(ReplayEvent::bootstrap)
            && !roots.get(0).bootstrap()) {
            throw conflict(
                "Bootstrap capture fact is not the chain root: "
                    + capture.captureId()
            );
        }

        Set<UUID> visited = new HashSet<>();
        ReplayEvent cursor = roots.get(0);
        while (cursor != null) {
            if (!visited.add(cursor.event().eventId())) {
                throw conflict(
                    "Capture chain contains a cycle: " + capture.captureId()
                );
            }
            cursor = successorByPredecessor.get(cursor.event().eventId());
        }
        if (visited.size() != events.size()) {
            throw conflict(
                "Capture chain is disconnected or cyclic: "
                    + capture.captureId()
            );
        }
        return events.stream()
            .filter(event ->
                !successorByPredecessor.containsKey(event.event().eventId())
            )
            .reduce((first, second) -> {
                throw conflict(
                    "Capture chain has more than one head: "
                        + capture.captureId()
                );
            })
            .orElseThrow(() ->
                conflict("Capture chain has no head: " + capture.captureId())
            );
    }

    private long scanEveryCaptureEvent() {
        long afterPosition = 0;
        long count = 0;
        while (true) {
            List<JournalEvent> events = jdbc.query(
                """
                    SELECT
                        journal_position,
                        event_id,
                        event_type,
                        shape_ref,
                        activity_ref,
                        subject_type,
                        subject_id,
                        actor_id,
                        recorded_at,
                        payload::text AS payload
                    FROM event_journal
                    WHERE event_type = ?
                      AND journal_position > ?
                    ORDER BY journal_position
                    LIMIT ?
                    """,
                (resultSet, rowNumber) -> mapEvent(resultSet),
                CaptureShadowProtocol.CAPTURE_EVENT_TYPE,
                afterPosition,
                PAGE_SIZE
            );
            if (events.isEmpty()) {
                return count;
            }
            requireSingleOwners(events);
            count += events.size();
            afterPosition = events.get(events.size() - 1)
                .journalPosition();
        }
    }

    private void requireSingleOwners(List<JournalEvent> events) {
        Map<UUID, String> bootstrapOwners = new LinkedHashMap<>();
        Set<UUID> liveOwners = new HashSet<>();
        for (JournalEvent event : events) {
            if (CaptureShadowProtocol.CAPTURE_SHAPE_REF.equals(
                event.shapeRef()
            )) {
                JsonNode submission = event.payload().path("submission");
                if (!submission.isObject()
                    || !submission.path("uid").isTextual()) {
                    throw conflict(
                        "Orphan bootstrap capture event: "
                            + event.eventId()
                    );
                }
                String uid = submission.path("uid").textValue();
                if (!CaptureShadowProtocol.captureEventId(uid)
                    .equals(event.eventId())) {
                    throw conflict(
                        "Bootstrap capture event has unrelated identity: "
                            + event.eventId()
                    );
                }
                bootstrapOwners.put(
                    CaptureShadowProtocol.captureId(uid),
                    uid
                );
            } else if (CaptureShadowProtocol.LIVE_CAPTURE_SHAPE_REF.equals(
                event.shapeRef()
            )) {
                liveOwners.add(parseUuid(
                    event.payload().path("captureId").textValue(),
                    "live capture ID"
                ));
            } else {
                throw conflict("Unknown capture shape: " + event.shapeRef());
            }
        }
        Set<UUID> captureIds = new HashSet<>(bootstrapOwners.keySet());
        captureIds.addAll(liveOwners);
        Set<String> submissionUids =
            new HashSet<>(bootstrapOwners.values());
        List<CaptureIdentityLink> identities = namedJdbc.query(
            """
                SELECT capture_id, baseline_submission_uid,
                       baseline_submission_id, baseline_serial_number
                FROM capture_identity_link
                WHERE capture_id IN (:captureIds)
                   OR baseline_submission_uid IN (:submissionUids)
                """,
            new MapSqlParameterSource()
                .addValue("captureIds", captureIds)
                .addValue(
                    "submissionUids",
                    submissionUids.isEmpty()
                        ? Set.of("__none__")
                        : submissionUids
                ),
            (resultSet, rowNumber) -> new CaptureIdentityLink(
                resultSet.getObject("capture_id", UUID.class),
                resultSet.getString("baseline_submission_uid"),
                resultSet.getString("baseline_submission_id"),
                resultSet.getLong("baseline_serial_number")
            )
        );
        for (UUID captureId : captureIds) {
            List<CaptureIdentityLink> matching = identities.stream()
                .filter(identity ->
                    identity.captureId().equals(captureId)
                        || Objects.equals(
                            identity.baselineSubmissionUid(),
                            bootstrapOwners.get(captureId)
                        )
                )
                .toList();
            if (matching.size() != 1
                || !matching.get(0).captureId().equals(captureId)
                || (bootstrapOwners.containsKey(captureId)
                    && !matching.get(0).baselineSubmissionUid().equals(
                        bootstrapOwners.get(captureId)
                    ))) {
                throw conflict(
                    "Capture event has conflicting or missing ownership: "
                        + captureId
                );
            }
        }
    }

    private BootstrapBoundary bootstrapBoundary() {
        var checkpoint = checkpoints.find(
            CaptureShadowProtocol.CHECKPOINT_KEY
        );
        if (checkpoint.isEmpty()) {
            return new BootstrapBoundary(false, null);
        }
        JsonNode payload = checkpoint.orElseThrow().payload();
        if (!payload.isObject()
            || payload.size() != 3
            || !payload.path("sourceCount").isIntegralNumber()
            || !payload.path("sourceCount").canConvertToLong()
            || payload.path("sourceCount").longValue() < 0
            || !payload.has("sourceMaxSerial")
            || !payload.path("sourceSha256").isTextual()
            || !payload.path("sourceSha256").textValue()
            .matches("[0-9a-f]{64}")) {
            throw conflict("Capture bootstrap checkpoint is malformed");
        }
        JsonNode value = payload.path("sourceMaxSerial");
        if (value.isNull()) {
            if (payload.path("sourceCount").longValue() != 0) {
                throw conflict(
                    "Capture bootstrap checkpoint is malformed"
                );
            }
            return new BootstrapBoundary(true, null);
        }
        if (!value.isIntegralNumber()
            || !value.canConvertToLong()
            || payload.path("sourceCount").longValue() == 0) {
            throw conflict(
                "Capture bootstrap checkpoint is malformed"
            );
        }
        return new BootstrapBoundary(true, value.longValue());
    }

    private void validateCanonicalSubmission(JsonNode submission) {
        if (!submission.isObject()
            || !fieldNames(submission).equals(SUBMISSION_FIELDS)
            || !submission.path("uid").isTextual()
            || !(submission.path("deleted").isBoolean()
                || submission.path("deleted").isNull())
            || !(submission.path("formData").isObject()
                || submission.path("formData").isNull())
            || !(submission.path("formVersionNumber").isIntegralNumber()
                || submission.path("formVersionNumber").isNull())) {
            throw conflict("Canonical capture submission is malformed");
        }
        for (String field : SUBMISSION_FIELDS) {
            if (Set.of(
                "deleted",
                "formData",
                "formVersionNumber"
            ).contains(field)) {
                continue;
            }
            JsonNode value = submission.path(field);
            if (!(value.isTextual() || value.isNull())) {
                throw conflict(
                    "Canonical capture submission field is malformed: "
                        + field
                );
            }
        }
    }

    private Map<UUID, UUID> uniqueUuidMap(
        List<Map.Entry<UUID, UUID>> entries,
        String label
    ) {
        Map<UUID, UUID> values = new LinkedHashMap<>();
        for (Map.Entry<UUID, UUID> entry : entries) {
            if (values.put(entry.getKey(), entry.getValue()) != null) {
                throw conflict("Conflicting " + label);
            }
        }
        return Map.copyOf(values);
    }

    private Set<String> fieldNames(JsonNode object) {
        Set<String> names = new HashSet<>();
        Iterator<String> fields = object.fieldNames();
        fields.forEachRemaining(names::add);
        return names;
    }

    private Set<String> differingFields(
        JsonNode eventSubmission,
        JsonNode currentSubmission
    ) {
        Set<String> fields = fieldNames(eventSubmission);
        fields.addAll(fieldNames(currentSubmission));
        return fields.stream()
            .filter(field -> !Objects.equals(
                eventSubmission.get(field),
                currentSubmission.get(field)
            ))
            .collect(Collectors.toCollection(java.util.TreeSet::new));
    }

    private void requireCount(String table, long expected) {
        Long actual = jdbc.queryForObject(
            "SELECT count(*) FROM " + table,
            Long.class
        );
        if (actual == null || actual != expected) {
            throw conflict(
                table + " count differs expected=" + expected
                    + " actual=" + actual
            );
        }
    }

    private JournalEvent mapEvent(ResultSet resultSet) throws SQLException {
        return new JournalEvent(
            resultSet.getLong("journal_position"),
            resultSet.getObject("event_id", UUID.class),
            resultSet.getString("event_type"),
            resultSet.getString("shape_ref"),
            resultSet.getString("activity_ref"),
            resultSet.getString("subject_type"),
            resultSet.getObject("subject_id", UUID.class),
            resultSet.getString("actor_id"),
            resultSet.getTimestamp("recorded_at").toInstant(),
            readJson(resultSet.getString("payload"))
        );
    }

    private JsonNode readJson(String value) {
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException exception) {
            throw conflict("Capture journal payload is invalid JSON", exception);
        }
    }

    private String nullableText(JsonNode value, String field) {
        if (value.isNull()) {
            return null;
        }
        if (!value.isTextual()) {
            throw conflict(
                "Canonical capture submission field is malformed: " + field
            );
        }
        return value.textValue();
    }

    private Instant parseInstant(String value, String label) {
        try {
            return Instant.parse(value);
        } catch (RuntimeException exception) {
            throw conflict("Invalid " + label, exception);
        }
    }

    private UUID parseUuid(String value, String label) {
        try {
            return UUID.fromString(value);
        } catch (RuntimeException exception) {
            throw conflict("Invalid " + label, exception);
        }
    }

    private CaptureReplayConflictException conflict(String message) {
        return new CaptureReplayConflictException(message);
    }

    private CaptureReplayConflictException conflict(
        String message,
        Throwable cause
    ) {
        return new CaptureReplayConflictException(message, cause);
    }

    private static final class PageRequirements {
        private final Set<OrgUnitAlias> orgUnitAliases =
            new HashSet<>();
        private final List<AcceptanceCheck> acceptances =
            new ArrayList<>();
    }

    private record ExpectedCapture(
        CaptureSourceRow row,
        CanonicalCapture capture
    ) {
    }

    private record ReplayEvent(
        JournalEvent event,
        UUID previousEventId,
        JsonNode submission,
        boolean bootstrap
    ) {
    }

    private record ReplayPageResult(
        long eventCount,
        long pointersInserted
    ) {
    }

    private record PointerInsert(
        UUID captureId,
        UUID sourceEventId
    ) {
    }

    private record OrgUnitAlias(
        UUID orgUnitId,
        String baselineOrgUnitUid
    ) {
    }

    private record BootstrapBoundary(
        boolean present,
        Long maximumSerial
    ) {
    }
}
