package org.nmcpye.datarun.web.rest.v1.intake;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.common.EntitySaveSummaryVM;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
import org.nmcpye.datarun.jpa.intake.GoLiveRuleService;
import org.nmcpye.datarun.jpa.intake.HeldFilter;
import org.nmcpye.datarun.jpa.intake.HeldState;
import org.nmcpye.datarun.jpa.intake.HeldSubmissionRepository;
import org.nmcpye.datarun.jpa.intake.HeldSubmissionRow;
import org.nmcpye.datarun.security.DomainUserDetailsService;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.rest.v1.datasubmission.dto.DataSubmissionUploadV1Dto;
import org.nmcpye.datarun.web.rest.v1.datasubmission.service.SubmissionUploadService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * An admin's decisions on held uploads (DataRun-ye/data-run-api#42).
 *
 * <p>Releasing replays the stored upload through the normal upload path, as
 * its original submitter, so it is checked again against today's assignments
 * and permissions and saved exactly like a live upload. A held upload whose
 * submission already exists in the real data is marked superseded instead,
 * so a release never overwrites real data. Discarding only labels the upload;
 * a discarded upload can still be released.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HeldSubmissionReview {

    private static final Set<HeldState> RELEASABLE = EnumSet.of(HeldState.HELD, HeldState.DISCARDED);
    private static final Set<HeldState> DISCARDABLE = EnumSet.of(HeldState.HELD);

    private final HeldSubmissionRepository held;
    private final GoLiveRuleService goLiveRules;
    private final DataSubmissionRepository submissions;
    private final SubmissionUploadService uploads;
    private final DomainUserDetailsService users;
    private final ObjectMapper objectMapper;

    public HeldDecisionResult release(HeldDecisionRequest request) {
        String admin = currentAdmin();
        HeldDecisionResult result = HeldDecisionResult.empty();
        for (HeldSubmissionRow row : select(request, RELEASABLE, true)) {
            if (Boolean.TRUE.equals(submissions.existsByUid(row.uid()))) {
                held.markDecided(row.uid(), HeldState.SUPERSEDED, admin, request.note());
                result.superseded().add(row.uid());
                continue;
            }
            String failure;
            try {
                DataSubmissionUploadV1Dto upload =
                    objectMapper.treeToValue(row.payload(), DataSubmissionUploadV1Dto.class);
                EntitySaveSummaryVM saved = runAs(row.submittedBy(), () -> uploads.release(upload));
                failure = saved.getFailed().values().stream().findFirst().orElse(null);
            } catch (Exception e) {
                failure = "Cannot release as " + row.submittedBy() + ": " + e.getMessage();
            }
            if (failure == null) {
                held.markDecided(row.uid(), HeldState.RELEASED, admin, request.note());
                result.released().add(row.uid());
                log.info("Held submission {} released by {}", row.uid(), admin);
            } else {
                held.recordReleaseError(row.uid(), failure);
                result.failed().put(row.uid(), failure);
                log.warn("Held submission {} not released by {}: {}", row.uid(), admin, failure);
            }
        }
        return result;
    }

    public HeldDecisionResult discard(HeldDecisionRequest request) {
        String admin = currentAdmin();
        HeldDecisionResult result = HeldDecisionResult.empty();
        for (HeldSubmissionRow row : select(request, DISCARDABLE, false)) {
            held.markDecided(row.uid(), HeldState.DISCARDED, admin, request.note());
            result.discarded().add(row.uid());
        }
        if (!result.discarded().isEmpty()) {
            log.info("{} held submissions discarded by {}", result.discarded().size(), admin);
        }
        return result;
    }

    private List<HeldSubmissionRow> select(HeldDecisionRequest request, Set<HeldState> states, boolean withPayload) {
        boolean byUid = request.uids() != null && !request.uids().isEmpty();
        if (byUid == (request.filter() != null)) {
            throw new IllegalQueryException("Give either uids or filter");
        }
        if (byUid) {
            return held.findWithPayload(request.uids(), states);
        }
        HeldDecisionRequest.Filter filter = request.filter();
        if (filter.activity() == null) {
            throw new IllegalQueryException("filter.activity is required");
        }
        HeldFilter heldFilter = new HeldFilter(
            filter.activity(), filter.orgUnit(), filter.form(), filter.reason(), states,
            filter.enteredFrom(), filter.enteredTo());
        List<HeldSubmissionRow> rows = withPayload ? held.findWithPayload(heldFilter) : held.findAll(heldFilter);
        if (Boolean.TRUE.equals(filter.acceptedByCurrentRules())) {
            rows = rows.stream().filter(goLiveRules.acceptedByCurrentRules()).toList();
        }
        return rows;
    }

    /** Runs {@code work} as {@code login}, so its checks and audit fields are that user's. */
    private <R> R runAs(String login, Supplier<R> work) {
        UserDetails submitter = users.loadUserByUsername(login);
        SecurityContext previous = SecurityContextHolder.getContext();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(submitter, null, submitter.getAuthorities()));
        SecurityContextHolder.setContext(context);
        try {
            return work.get();
        } finally {
            SecurityContextHolder.setContext(previous);
        }
    }

    private static String currentAdmin() {
        return SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new IllegalStateException("No admin in the security context"));
    }

    /** What a release or discard did, per uid. */
    public record HeldDecisionResult(
        List<String> released,
        List<String> discarded,
        List<String> superseded,
        Map<String, String> failed) {

        static HeldDecisionResult empty() {
            return new HeldDecisionResult(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new LinkedHashMap<>());
        }
    }
}
