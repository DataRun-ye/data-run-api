package org.nmcpye.datarun.assignmentshadow;

import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Component
public class VersionedUploadEventAuthorizer {

    private static final Logger log = LoggerFactory.getLogger(
        VersionedUploadEventAuthorizer.class
    );

    private final LatestAssignmentGrantReader latestGrantReader;
    private final AssignmentCaptureScopeFactory scopeFactory;
    private final BaselineVersionedUploadCompatibilityAdapter compatibility;

    public VersionedUploadEventAuthorizer(
        LatestAssignmentGrantReader latestGrantReader,
        AssignmentCaptureScopeFactory scopeFactory,
        BaselineVersionedUploadCompatibilityAdapter compatibility
    ) {
        this.latestGrantReader = latestGrantReader;
        this.scopeFactory = scopeFactory;
        this.compatibility = compatibility;
    }

    public Session openSession(
        CurrentUserDetails user,
        Collection<String> rawAssignmentUids
    ) {
        LinkedHashSet<String> assignments = new LinkedHashSet<>();
        if (rawAssignmentUids != null) {
            rawAssignmentUids.stream()
                .filter(Objects::nonNull)
                .forEach(assignments::add);
        }
        return new Session(user, Set.copyOf(assignments));
    }

    public final class Session {

        private final CurrentUserDetails user;
        private final Set<String> assignmentUids;
        private LatestAssignmentGrantSnapshot snapshot;

        private Session(
            CurrentUserDetails user,
            Set<String> assignmentUids
        ) {
            this.user = Objects.requireNonNull(user);
            this.assignmentUids = assignmentUids;
        }

        public void authorize(
            Assignment assignment,
            String formUid,
            String submissionUid
        ) {
            if (user.isSuper()) {
                return;
            }

            EventDecision decision = decideSafely(assignment, formUid);
            if (decision == EventDecision.ACTIVE_GRANT
                || decision == EventDecision.ENDED_RETIRED_ASSIGNMENT) {
                return;
            }
            if (decision == EventDecision.AUTHORITY_UNAVAILABLE) {
                throw new AssignmentCaptureAuthorityUnavailableException();
            }

            BaselineVersionedUploadCompatibilityAdapter.DenialClassification
                baseline;
            try {
                baseline = compatibility.classifyDenial(
                    user,
                    assignment,
                    formUid
                );
            } catch (RuntimeException exception) {
                log.warn(
                    "assignment_capture_upload_authority baseline_status=failed failure_type={}",
                    exception.getClass().getSimpleName()
                );
                throw new AssignmentCaptureAuthorityUnavailableException();
            }
            String teamUid = assignment.getTeam().getUid();
            if (baseline
                == BaselineVersionedUploadCompatibilityAdapter
                    .DenialClassification.NOT_DIRECT_TEAM) {
                throw new IllegalQueryException(
                    ErrorCode.E4114,
                    teamUid,
                    submissionUid
                );
            }
            if (baseline
                == BaselineVersionedUploadCompatibilityAdapter
                    .DenialClassification.NO_CAPTURE_PERMISSION) {
                throw new IllegalQueryException(ErrorCode.E1112, teamUid);
            }

            log.warn(
                "assignment_capture_upload_authority status=invariant_failure event_decision={} baseline_decision={}",
                decision,
                baseline
            );
            throw new AssignmentCaptureAuthorityUnavailableException();
        }

        private EventDecision decideSafely(
            Assignment assignment,
            String formUid
        ) {
            try {
                return decide(assignment, formUid);
            } catch (RuntimeException exception) {
                log.warn(
                    "assignment_capture_upload_authority decision_status=failed failure_type={}",
                    exception.getClass().getSimpleName()
                );
                return EventDecision.AUTHORITY_UNAVAILABLE;
            }
        }

        private EventDecision decide(
            Assignment assignment,
            String formUid
        ) {
            LatestAssignmentGrantSnapshot current = snapshot();
            if (current.status()
                == LatestAssignmentGrantSnapshot.Status.AUTHORITY_UNAVAILABLE) {
                return EventDecision.AUTHORITY_UNAVAILABLE;
            }

            AssignmentCaptureEventGrant latest = current
                .latestGrant(assignment.getUid())
                .orElse(null);
            if (latest == null) {
                return EventDecision.NO_GRANT;
            }
            var structuralScope = scopeFactory.fromAssignment(
                user,
                assignment,
                List.of(formUid)
            );
            if (!Boolean.TRUE.equals(assignment.getDeleted())
                && structuralScope.isPresent()
                && latest.lifecycleState() == AssignmentLifecycleState.ACTIVE
                && latest.matches(structuralScope.get(), formUid)) {
                return EventDecision.ACTIVE_GRANT;
            }

            var retiredScope = compatibility.retiredScope(user, assignment);
            if (retiredScope.isPresent()
                && retiredScope.get().formUids().contains(formUid)
                && latest.lifecycleState() == AssignmentLifecycleState.ENDED
                && latest.scope().equals(retiredScope.get())) {
                return EventDecision.ENDED_RETIRED_ASSIGNMENT;
            }
            return EventDecision.REVOKED_HISTORY;
        }

        private LatestAssignmentGrantSnapshot snapshot() {
            if (snapshot != null) {
                return snapshot;
            }
            try {
                snapshot = latestGrantReader.readAssignments(
                    user.getUid(),
                    assignmentUids
                );
                if (snapshot == null) {
                    log.warn(
                        "assignment_capture_upload_authority reader_status=invalid_result"
                    );
                    snapshot = LatestAssignmentGrantSnapshot.unavailable();
                }
            } catch (RuntimeException exception) {
                log.warn(
                    "assignment_capture_upload_authority reader_status=failed failure_type={}",
                    exception.getClass().getSimpleName()
                );
                snapshot = LatestAssignmentGrantSnapshot.unavailable();
            }
            return snapshot;
        }
    }

    enum EventDecision {
        ACTIVE_GRANT,
        ENDED_RETIRED_ASSIGNMENT,
        REVOKED_HISTORY,
        NO_GRANT,
        AUTHORITY_UNAVAILABLE
    }
}
