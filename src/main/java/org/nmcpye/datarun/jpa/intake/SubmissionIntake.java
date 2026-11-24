package org.nmcpye.datarun.jpa.intake;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
import org.nmcpye.datarun.jpa.orgunit.OrgUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Decides what happens to an upload the server has checked, and keeps the
 * held ones (DataRun-ye/data-run-api#42). The upload path calls it for every
 * submission; a held submission is checked and saved exactly like a real one
 * and then rolled back, so it never reaches the real data, analytics or other
 * phones, and the Reference entries it defines are not created.
 */
@Service
@RequiredArgsConstructor
public class SubmissionIntake {

    private final GoLiveRuleRepository rules;
    private final HeldSubmissionRepository held;
    private final DataSubmissionRepository submissions;

    /**
     * Released uploads are saved and practice uploads held. A live upload is
     * held when it comes before its campaign's go-live (see {@link GoLivePolicy}),
     * unless the submission is already in the real data: an update of real
     * data is never held.
     *
     * @param submission the submission with its context set from its assignment
     * @param areaChain  the assignment's org unit and its ancestors, nearest first; see {@link #areaChainOf}
     */
    public IntakeDecision decide(
        IntakeMode mode,
        DataSubmission submission,
        List<String> areaChain,
        String submittedBy,
        Instant receivedAt) {
        if (mode == IntakeMode.RELEASE) {
            return IntakeDecision.save();
        }
        HeldContext context = new HeldContext(
            submission.getActivity(),
            submission.getOrgUnit(),
            pathOf(areaChain),
            submission.getForm(),
            submission.getFormVersion(),
            submission.getTeam(),
            submission.getAssignment(),
            submittedBy,
            submission.getStartEntryTime(),
            receivedAt);
        if (mode == IntakeMode.PRACTICE) {
            return IntakeDecision.hold(HoldReason.PRACTICE, null, context);
        }

        Optional<GoLiveRule> rule = GoLivePolicy.applicableRule(
            rules.findByActivity(submission.getActivity()), areaChain, submission.getForm());
        if (rule.isEmpty()
            || !GoLivePolicy.beforeGoLive(rule.get(), submission.getStartEntryTime(), receivedAt)
            || Boolean.TRUE.equals(submissions.existsByUid(submission.getUid()))) {
            return IntakeDecision.save();
        }
        return IntakeDecision.hold(HoldReason.BEFORE_GO_LIVE, rule.get(), context);
    }

    /**
     * Keeps a held upload in its own transaction, after its checked save was rolled back.
     *
     * @return false when the held copy was already released or superseded, so nothing was kept
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean hold(String uid, JsonNode payload, IntakeDecision decision) {
        return held.hold(uid, decision.holdReason(), decision.rule(), decision.context(), payload);
    }

    /**
     * An org unit and its ancestors, nearest first, read from its parents.
     * Not {@link OrgUnit#getPath()}: that getter assigns the mapped path, so
     * calling it in a committing transaction can rewrite the org unit's row.
     */
    public static List<String> areaChainOf(OrgUnit orgUnit) {
        List<String> chain = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (OrgUnit unit = orgUnit; unit != null && seen.add(unit.getUid()); unit = unit.getParent()) {
            chain.add(unit.getUid());
        }
        return chain;
    }

    /** The org unit and its ancestors, nearest first, whether or not the path lists the unit itself. */
    static List<String> areaChain(String orgUnit, String orgUnitPath) {
        List<String> chain = new ArrayList<>(GoLivePolicy.areaChain(orgUnitPath));
        if (orgUnit != null && (chain.isEmpty() || !chain.get(0).equals(orgUnit))) {
            chain.add(0, orgUnit);
        }
        return chain;
    }

    /** A path in the stored form: uids from the root down, comma separated, starting with a comma. */
    static String pathOf(List<String> areaChain) {
        if (areaChain.isEmpty()) {
            return null;
        }
        List<String> rootFirst = new ArrayList<>(areaChain);
        Collections.reverse(rootFirst);
        return "," + String.join(",", rootFirst);
    }
}
