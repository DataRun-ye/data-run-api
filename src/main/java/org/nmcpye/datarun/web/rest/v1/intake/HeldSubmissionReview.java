package org.nmcpye.datarun.web.rest.v1.intake;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.jpa.datasubmission.command.HeldDecisionResult;
import org.nmcpye.datarun.jpa.datasubmission.command.HeldSubmissionDecisions;
import org.nmcpye.datarun.jpa.datasubmission.command.SubmissionActor;
import org.nmcpye.datarun.jpa.datasubmission.command.SubmissionCommandService;
import org.springframework.stereotype.Service;

/**
 * Transitional: the held release, not yet moved onto {@link SubmissionCommandService}, reaches the moved decision
 * code through here, as the admin of the current request. Removed when the release moves.
 */
@Service
@RequiredArgsConstructor
public class HeldSubmissionReview {

    private final HeldSubmissionDecisions decisions;

    /** See {@link HeldSubmissionDecisions#release}. */
    public HeldDecisionResult release(HeldDecisionRequest request) {
        return decisions.release(SubmissionActor.current(), request);
    }
}
