package org.nmcpye.datarun.jpa.datasubmission.command;

import org.nmcpye.datarun.web.rest.v1.datasubmission.dto.DataSubmissionUploadV1Dto;

import java.util.List;

/**
 * The writes {@link SubmissionCommandService} accepts, each naming the user it acts as. Items keep the shape the
 * route received: the v1 upload DTO is a frozen phone contract.
 */
public sealed interface SubmissionCommand {

    SubmissionActor actor();

    /** Phone uploads on the versioned route ({@code /bulk?referenceVersion=1}): apps 6.0.2 and later. */
    record Upload(SubmissionActor actor, List<DataSubmissionUploadV1Dto> items) implements SubmissionCommand {
    }

    /**
     * Uploads from a practice session ({@code /bulk/practice?referenceVersion=1}, apps 6.1.0 and later): checked like
     * an {@link Upload}, then kept apart as practice instead of saved.
     */
    record PracticeUpload(SubmissionActor actor, List<DataSubmissionUploadV1Dto> items) implements SubmissionCommand {
    }
}
