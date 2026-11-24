package org.nmcpye.datarun.jpa.datasubmission.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
import org.nmcpye.datarun.jpa.intake.GoLiveRuleService;
import org.nmcpye.datarun.jpa.intake.HeldFilter;
import org.nmcpye.datarun.jpa.intake.HeldState;
import org.nmcpye.datarun.jpa.intake.HeldSubmissionRepository;
import org.nmcpye.datarun.jpa.intake.HeldSubmissionRow;
import org.nmcpye.datarun.jpa.intake.HoldReason;
import org.nmcpye.datarun.security.DomainUserDetailsService;
import org.nmcpye.datarun.web.rest.v1.datasubmission.dto.DataSubmissionUploadV1Dto;
import org.nmcpye.datarun.web.rest.v1.intake.HeldDecisionRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * An admin's decisions on held uploads (DataRun-ye/data-run-api#42), for {@link SubmissionCommandService}.
 *
 * <p>Releasing replays the stored upload through the normal upload path ({@link SubmissionUploads}) on behalf of its
 * original submitter, so it is checked again against today's assignments and permissions and saved exactly like a
 * live upload: its records name the submitter, and the decision names the admin. A held upload whose submission
 * already exists in the real data is marked superseded instead, so a release never overwrites real data. Discarding
 * only labels the upload; a discarded upload can still be released.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HeldSubmissionDecisions {

    /** Held or discarded: what a release by uid may take. Released and superseded uploads are final. */
    private static final Set<HeldState> UNDECIDED_OR_DISCARDED = EnumSet.of(HeldState.HELD, HeldState.DISCARDED);
    private static final Set<HeldState> UNDECIDED = EnumSet.of(HeldState.HELD);
    static final int DEFAULT_BATCH = 200;
    static final int MAX_BATCH = 500;

    private final HeldSubmissionRepository held;
    private final GoLiveRuleService goLiveRules;
    private final DataSubmissionRepository submissions;
    private final SubmissionUploads uploads;
    private final DomainUserDetailsService users;
    private final ObjectMapper objectMapper;

    /**
     * Releases held uploads into the real data. By uid it takes held or
     * discarded uploads of either reason. By filter it takes only uploads still
     * held because they came before a go-live: discarded uploads and practice
     * uploads are never released in bulk. At most one batch per call; the
     * result says how many matching uploads remain.
     *
     * <p>Each upload's data and the decision that released it commit together: when the held upload changed after
     * it was read (a phone sent it again), nothing is saved and it is reported as changed, so the next release
     * applies the newer copy. Its uid is checked against the real data before the replay and again by the replay's
     * own save, in the same transaction as the decision: a live upload that landed in between makes the held copy
     * superseded instead of overwriting it.</p>
     */
    public HeldDecisionResult release(SubmissionActor admin, HeldDecisionRequest request) {
        Selection selection = select(request, UNDECIDED_OR_DISCARDED, UNDECIDED, true);
        HeldDecisionResult result = HeldDecisionResult.empty(selection.remaining());
        for (HeldSubmissionRow row : held.findWithPayload(selection.uids(), selection.states())) {
            if (Boolean.TRUE.equals(submissions.existsByUid(row.uid()))) {
                record(result, row, HeldState.SUPERSEDED, admin, request.note(), result.superseded());
                continue;
            }
            String failure;
            String[] uploader = {null};
            SubmissionUploads.ReleaseOutcome outcome = SubmissionUploads.ReleaseOutcome.FAILED;
            try {
                DataSubmissionUploadV1Dto upload =
                    objectMapper.treeToValue(row.payload(), DataSubmissionUploadV1Dto.class);
                SubmissionUploads.Released released = onBehalfOf(row.submittedBy(), submitter -> {
                    uploader[0] = submitter.userId();
                    return uploads.saveReleased(upload, submitter, () -> held.markDecided(row.uid(),
                        HeldState.RELEASED, UNDECIDED_OR_DISCARDED, row.lastModifiedDate(), admin.login(),
                        request.note()));
                });
                outcome = released.outcome();
                failure = released.failure();
            } catch (Exception e) {
                failure = SubmissionLog.code(e);
            }
            if (failure != null) {
                // What admins read, in last_release_error and the answer: a code or an exception type, and the
                // uploader's user id; never the login or an exception's message.
                failure += " (uploader " + (uploader[0] == null ? "not loaded" : uploader[0]) + ")";
            }
            if (outcome == SubmissionUploads.ReleaseOutcome.SUPERSEDED) {
                record(result, row, HeldState.SUPERSEDED, admin, request.note(), result.superseded());
            } else if (failure == null) {
                (outcome == SubmissionUploads.ReleaseOutcome.RELEASED ? result.released() : result.changed())
                    .add(row.uid());
            } else {
                held.recordReleaseError(row.uid(), failure);
                result.failed().put(row.uid(), failure);
                log.warn("Held submission {} (form {}) not released by user {}: {}",
                    row.uid(), row.form(), admin.userId(), failure);
            }
        }
        if (!result.released().isEmpty()) {
            log.info("{} held submissions released by user {}", result.released().size(), admin.userId());
        }
        return result;
    }

    /** Labels held uploads as not real; they stay stored and can still be released by uid. */
    public HeldDecisionResult discard(SubmissionActor admin, HeldDecisionRequest request) {
        Selection selection = select(request, UNDECIDED, UNDECIDED, false);
        HeldDecisionResult result = HeldDecisionResult.empty(selection.remaining());
        for (HeldSubmissionRow row : held.findWithPayload(selection.uids(), selection.states())) {
            record(result, row, HeldState.DISCARDED, admin, request.note(), result.discarded());
        }
        if (!result.discarded().isEmpty()) {
            log.info("{} held submissions discarded by user {}", result.discarded().size(), admin.userId());
        }
        return result;
    }

    /** Records a decision, in the admin's name, only if nothing changed the upload since it was read. */
    private void record(
        HeldDecisionResult result, HeldSubmissionRow row, HeldState state, SubmissionActor admin, String note,
        List<String> outcome) {
        if (held.markDecided(row.uid(), state, UNDECIDED_OR_DISCARDED, row.lastModifiedDate(), admin.login(), note)) {
            outcome.add(row.uid());
        } else {
            result.changed().add(row.uid());
        }
    }

    /** The uids to act on now, the states they may be in, and how many matching uploads wait for later calls. */
    private record Selection(List<String> uids, Set<HeldState> states, long remaining) {
    }

    private Selection select(
        HeldDecisionRequest request, Set<HeldState> byUidStates, Set<HeldState> byFilterStates, boolean release) {
        boolean byUid = request.uids() != null && !request.uids().isEmpty();
        if (byUid == (request.filter() != null)) {
            throw new IllegalQueryException("Give either uids or filter");
        }
        int batch = request.limit() == null ? DEFAULT_BATCH : Math.min(Math.max(request.limit(), 1), MAX_BATCH);
        if (byUid) {
            List<String> uids = request.uids().stream().distinct().toList();
            return new Selection(uids.stream().limit(batch).toList(), byUidStates, Math.max(uids.size() - batch, 0));
        }

        HeldDecisionRequest.Filter filter = request.filter();
        if (filter.activity() == null) {
            throw new IllegalQueryException("filter.activity is required");
        }
        HoldReason reason = filter.reason();
        if (release) {
            if (reason == HoldReason.PRACTICE) {
                throw new IllegalQueryException("Practice uploads are released only by uid");
            }
            reason = HoldReason.BEFORE_GO_LIVE;
        }
        List<HeldSubmissionRow> matching = held.findAll(new HeldFilter(
            filter.activity(), filter.orgUnit(), filter.form(), reason, byFilterStates,
            filter.enteredFrom(), filter.enteredTo()));
        if (Boolean.TRUE.equals(filter.acceptedByCurrentRules())) {
            matching = matching.stream().filter(goLiveRules.acceptedByCurrentRules()).toList();
        }
        List<String> uids = matching.stream().limit(batch).map(HeldSubmissionRow::uid).toList();
        return new Selection(uids, byFilterStates, Math.max(matching.size() - batch, 0));
    }

    /**
     * Runs {@code work} on behalf of the submitter whose login the held upload stores: the checks read that user's
     * access, and the security context is theirs while it runs, so the audit columns name them. The admin's context
     * comes back afterwards.
     */
    private <R> R onBehalfOf(String login, Function<SubmissionActor, R> work) {
        UserDetails submitter = users.loadUserByUsername(login);
        SecurityContext previous = SecurityContextHolder.getContext();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(submitter, null, submitter.getAuthorities()));
        SecurityContextHolder.setContext(context);
        try {
            return work.apply(SubmissionActor.current());
        } finally {
            SecurityContextHolder.setContext(previous);
        }
    }
}
