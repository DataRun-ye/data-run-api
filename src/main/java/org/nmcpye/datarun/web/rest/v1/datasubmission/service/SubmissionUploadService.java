package org.nmcpye.datarun.web.rest.v1.datasubmission.service;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.common.EntitySaveSummaryVM;
import org.nmcpye.datarun.jpa.datasubmission.command.SubmissionActor;
import org.nmcpye.datarun.jpa.datasubmission.command.SubmissionCommandService;
import org.nmcpye.datarun.jpa.datasubmission.command.SubmissionUploads;
import org.nmcpye.datarun.jpa.intake.IntakeMode;
import org.nmcpye.datarun.web.rest.v1.datasubmission.dto.DataSubmissionUploadV1Dto;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Transitional: the upload paths not yet moved onto {@link SubmissionCommandService} reach the moved upload code
 * through here, as the user of the current request. Removed when the last of them moves.
 */
@Service
@RequiredArgsConstructor
public class SubmissionUploadService {

    private final SubmissionUploads uploads;

    /**
     * Saves a held upload an admin released, through the same path as a live
     * upload. The caller runs it as the upload's original submitter.
     */
    public EntitySaveSummaryVM release(DataSubmissionUploadV1Dto request) {
        return uploads.save(List.of(request), SubmissionActor.current(), IntakeMode.RELEASE);
    }
}
