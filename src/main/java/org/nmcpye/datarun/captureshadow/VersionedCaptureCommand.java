package org.nmcpye.datarun.captureshadow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.nmcpye.datarun.assignmentshadow.TransitionIdentityResolver;
import org.nmcpye.datarun.assignmentshadow.VersionedUploadAuthorityReceipt;
import org.nmcpye.datarun.common.EntitySaveSummaryVM;
import org.nmcpye.datarun.config.datarun.DatarunProperties;
import org.nmcpye.datarun.eventjournal.AppendJournalEvent;
import org.nmcpye.datarun.eventjournal.EventJournalPort;
import org.nmcpye.datarun.eventjournal.JournalEvent;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.service.DataSubmissionService;
import org.nmcpye.datarun.jpa.datasubmission.service.SubmissionMutationKind;
import org.nmcpye.datarun.jpa.datasubmission.service.SubmissionMutationResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class VersionedCaptureCommand {

    private final DataSubmissionService submissions;
    private final DatarunProperties properties;
    private final CaptureIdentityLinkPort identities;
    private final CaptureCurrentProjectionPort currentProjection;
    private final TransitionIdentityResolver transitionIdentities;
    private final CaptureSubmissionCanonicalizer canonicalizer;
    private final CaptureAcceptanceValidator acceptanceValidator;
    private final EventJournalPort journal;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public VersionedCaptureCommand(
        DataSubmissionService submissions,
        DatarunProperties properties,
        CaptureIdentityLinkPort identities,
        CaptureCurrentProjectionPort currentProjection,
        TransitionIdentityResolver transitionIdentities,
        CaptureSubmissionCanonicalizer canonicalizer,
        CaptureAcceptanceValidator acceptanceValidator,
        EventJournalPort journal,
        ObjectMapper objectMapper,
        Clock clock
    ) {
        this.submissions = submissions;
        this.properties = properties;
        this.identities = identities;
        this.currentProjection = currentProjection;
        this.transitionIdentities = transitionIdentities;
        this.canonicalizer = canonicalizer;
        this.acceptanceValidator = acceptanceValidator;
        this.journal = journal;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public void execute(
        List<VersionedCaptureSubmissionCommand> commands,
        EntitySaveSummaryVM summary
    ) {
        List<DataSubmission> requested = commands.stream()
            .map(VersionedCaptureSubmissionCommand::submission)
            .toList();
        List<SubmissionMutationResult> results =
            submissions.upsertAllClassified(requested, summary);
        if (!properties.getTransition().isCaptureLiveShadowEnabled()) {
            return;
        }

        Map<String, VersionedUploadAuthorityReceipt> authorities =
            authorities(commands);
        List<SubmissionMutationResult> mutations = results.stream()
            .filter(result ->
                result.kind() != SubmissionMutationKind.UNCHANGED
            )
            .sorted(Comparator.comparing(result ->
                CaptureShadowProtocol.captureId(
                    result.submission().getUid()
                )
            ))
            .toList();
        for (SubmissionMutationResult mutation : mutations) {
            append(
                mutation,
                Objects.requireNonNull(
                    authorities.get(mutation.submission().getUid()),
                    "Missing capture authority receipt"
                )
            );
        }
    }

    private void append(
        SubmissionMutationResult mutation,
        VersionedUploadAuthorityReceipt authority
    ) {
        DataSubmission submission = mutation.submission();
        requirePhysicalIdentity(submission);
        UUID captureId = CaptureShadowProtocol.captureId(submission.getUid());
        CaptureIdentityLink expectedIdentity = new CaptureIdentityLink(
            captureId,
            submission.getUid(),
            submission.getId(),
            submission.getSerialNumber()
        );

        UUID previousEventId;
        if (mutation.kind() == SubmissionMutationKind.CREATE) {
            var resolution = identities.resolveOrInsert(expectedIdentity);
            if (!resolution.inserted()) {
                throw conflict(
                    "Capture identity already exists for new submission "
                        + submission.getUid()
                );
            }
            previousEventId = null;
        } else {
            identities.requireExact(expectedIdentity);
            previousEventId = currentProjection
                .findSourceEventIdForUpdate(captureId)
                .orElseThrow(() ->
                    conflict(
                        "Missing capture current pointer for "
                            + submission.getUid()
                    )
                );
            requirePredecessor(captureId, submission.getUid(), previousEventId);
        }

        var actor = transitionIdentities.requireOrCreateActor(
            authority.baselineUserUid()
        );
        if (!actor.actorId().equals(authority.actorId())) {
            throw conflict("Authority receipt actor alias is not exact");
        }
        var orgUnit = transitionIdentities.requireOrCreateOrgUnit(
            submission.getOrgUnit()
        );

        ObjectNode acceptance = acceptance(authority);
        acceptanceValidator.validateCurrentGrant(
            acceptance,
            authority.actorId(),
            submission.getAssignment()
        );
        UUID eventId = UUID.randomUUID();
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("captureId", captureId.toString());
        if (previousEventId == null) {
            payload.putNull("previousEventId");
        } else {
            payload.put("previousEventId", previousEventId.toString());
        }
        payload.set("acceptance", acceptance);
        payload.set("submission", canonicalizer.canonicalize(submission));

        journal.append(new AppendJournalEvent(
            eventId,
            CaptureShadowProtocol.CAPTURE_EVENT_TYPE,
            CaptureShadowProtocol.LIVE_CAPTURE_SHAPE_REF,
            submission.getActivity(),
            CaptureShadowProtocol.CAPTURE_SUBJECT_TYPE,
            orgUnit.orgUnitId(),
            authority.actorId().toString(),
            clock.instant(),
            payload
        ));
        if (mutation.kind() == SubmissionMutationKind.CREATE) {
            currentProjection.insertInitialPointer(captureId, eventId);
        } else {
            currentProjection.compareAndSwap(
                captureId,
                previousEventId,
                eventId
            );
        }
    }

    private void requirePredecessor(
        UUID captureId,
        String submissionUid,
        UUID eventId
    ) {
        JournalEvent event = journal.findByEventId(eventId)
            .orElseThrow(() ->
                conflict("Capture current pointer references a missing event")
            );
        boolean bootstrap = eventId.equals(
            CaptureShadowProtocol.captureEventId(submissionUid)
        ) && CaptureShadowProtocol.CAPTURE_SHAPE_REF.equals(event.shapeRef());
        boolean live = CaptureShadowProtocol.LIVE_CAPTURE_SHAPE_REF.equals(
            event.shapeRef()
        ) && captureId.toString().equals(
            event.payload().path("captureId").textValue()
        );
        if (!CaptureShadowProtocol.CAPTURE_EVENT_TYPE.equals(event.eventType())
            || (!bootstrap && !live)) {
            throw conflict(
                "Capture current pointer does not belong to " + captureId
            );
        }
    }

    private ObjectNode acceptance(
        VersionedUploadAuthorityReceipt authority
    ) {
        ObjectNode acceptance = objectMapper.createObjectNode();
        if (authority.acceptance()
            instanceof VersionedUploadAuthorityReceipt.AssignmentAcceptance assignment) {
            acceptance.put("kind", "assignment");
            acceptance.put(
                "grantEventId",
                assignment.grantEventId().toString()
            );
        } else if (authority.acceptance()
            instanceof VersionedUploadAuthorityReceipt.AdministratorAcceptance) {
            acceptance.put("kind", "administrator");
        } else {
            throw conflict("Unknown capture authority receipt");
        }
        return acceptance;
    }

    private Map<String, VersionedUploadAuthorityReceipt> authorities(
        List<VersionedCaptureSubmissionCommand> commands
    ) {
        Map<String, VersionedUploadAuthorityReceipt> values = new HashMap<>();
        for (VersionedCaptureSubmissionCommand command : commands) {
            String uid = command.submission().getUid();
            if (values.put(uid, command.authority()) != null) {
                throw conflict(
                    "Duplicate submission UID in capture command: " + uid
                );
            }
        }
        return Map.copyOf(values);
    }

    private void requirePhysicalIdentity(DataSubmission submission) {
        if (submission.getId() == null || submission.getSerialNumber() == null) {
            throw conflict(
                "Persisted submission has no final physical identity: "
                    + submission.getUid()
            );
        }
    }

    private CaptureShadowConflictException conflict(String message) {
        return new CaptureShadowConflictException(message);
    }
}
