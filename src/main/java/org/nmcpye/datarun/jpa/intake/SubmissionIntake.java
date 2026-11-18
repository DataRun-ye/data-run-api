package org.nmcpye.datarun.jpa.intake;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
     * @param submission  the submission with its context set from its assignment
     * @param orgUnitPath the path of the assignment's org unit
     */
    public IntakeDecision decide(
        IntakeMode mode,
        DataSubmission submission,
        String orgUnitPath,
        String submittedBy,
        Instant receivedAt) {
        if (mode == IntakeMode.RELEASE) {
            return IntakeDecision.save();
        }
        List<String> areaChain = areaChain(submission.getOrgUnit(), orgUnitPath);
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

    /** Keeps a held upload in its own transaction, after its checked save was rolled back. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void hold(String uid, JsonNode payload, IntakeDecision decision) {
        held.hold(uid, decision.holdReason(), decision.rule(), decision.context(), payload);
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
