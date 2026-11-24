package org.nmcpye.datarun.jpa.datasubmission.command;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.common.EntitySaveSummaryVM;
import org.nmcpye.datarun.jpa.intake.IntakeMode;
import org.springframework.stereotype.Service;

/**
 * The one place where submissions are created, changed, deleted, held or released. Controllers adapt their routes to
 * a {@link SubmissionCommand}; every command runs one set of checks, writes each submission in a transaction of its
 * own, and writes the submission's outbox row in that same transaction.
 */
@Service
@RequiredArgsConstructor
public class SubmissionCommandService {

    private final SubmissionUploads uploads;

    /**
     * Phone uploads on the versioned route. Each submission is checked and saved in its own transaction, or held
     * when it comes before its campaign's go-live; either way the phone is told it was saved.
     */
    public EntitySaveSummaryVM upload(SubmissionCommand.Upload command) {
        return uploads.save(command.items(), command.actor(), IntakeMode.LIVE);
    }
}
