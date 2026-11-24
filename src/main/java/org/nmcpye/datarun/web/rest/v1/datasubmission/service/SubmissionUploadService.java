package org.nmcpye.datarun.web.rest.v1.datasubmission.service;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.common.EntitySaveSummaryVM;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.command.SubmissionActor;
import org.nmcpye.datarun.jpa.datasubmission.command.SubmissionCommandService;
import org.nmcpye.datarun.jpa.datasubmission.command.SubmissionUploads;
import org.nmcpye.datarun.jpa.intake.IntakeMode;
import org.nmcpye.datarun.web.rest.v1.datasubmission.dto.DataSubmissionUploadV1Dto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Transitional: the upload paths not yet moved onto {@link SubmissionCommandService} reach the moved upload code
 * through here, as the user of the current request. Removed when the last of them moves.
 */
@Service
@RequiredArgsConstructor
public class SubmissionUploadService {

    private final SubmissionUploads uploads;

    /** The practice route; see {@link SubmissionUploads#save}. */
    public EntitySaveSummaryVM upsertAll(
        List<DataSubmissionUploadV1Dto> requests,
        boolean practice) {
        return uploads.save(requests, SubmissionActor.current(), practice ? IntakeMode.PRACTICE : IntakeMode.LIVE);
    }

    /**
     * Saves a held upload an admin released, through the same path as a live
     * upload. The caller runs it as the upload's original submitter.
     */
    public EntitySaveSummaryVM release(DataSubmissionUploadV1Dto request) {
        return uploads.save(List.of(request), SubmissionActor.current(), IntakeMode.RELEASE);
    }

    /** The legacy route; see {@link SubmissionUploads#saveLegacy}. */
    public EntitySaveSummaryVM upsertAllLegacy(
        List<DataSubmission> submissions,
        UnaryOperator<DataSubmission> validate) {
        return uploads.saveLegacy(submissions, SubmissionActor.current(), validate);
    }
}
