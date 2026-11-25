package org.nmcpye.datarun.jpa.datasubmission.conflict;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.common.exceptions.FailureShape;
import org.nmcpye.datarun.config.activation.ActivationSwitches;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.revision.SubmissionRevisionReader;
import org.nmcpye.datarun.jpa.datasubmission.validation.StoredSubmissionGuard;
import org.nmcpye.datarun.jpa.datasubmissionbatching.job.MigrationRepeatIdGenerator;
import org.nmcpye.datarun.jpa.datatemplate.TemplateVersionContext;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static org.nmcpye.datarun.jpa.datasubmission.conflict.ConflictComparison.sameState;

/**
 * Conflict detection (spec, Conflicts), behind its activation switch ({@link ConflictDetectionSwitch}). For each
 * upload item of a stored submission, under the submission lock and before the upload checks, it tags a copy whose
 * rows lack ids (before any id is minted), finds the uploader's base version in the journal, and compares the copy.
 * The upload then follows the {@link ConflictCheck} it returns. Off, or for a new submission, the check is
 * {@link ConflictCheck#OFF} and the upload runs exactly as before.
 *
 * <p><b>The base version</b> is the uploader's newest revision of the submission: their {@code UPLOAD} or
 * {@code LEGACY_UPLOAD}, a {@code HELD_RELEASE} on their behalf, a plain {@code CONFLICT_ACCEPT} of their copy, or a
 * baseline of the live submission when they are its creator ({@link StoredSubmissionGuard#madeBy}). An uploader with
 * none has no base version of their own.</p>
 *
 * <p><b>Server-side changes</b> are revisions their phone did not make: {@code EDIT}, {@code MOVE}, {@code SPLIT},
 * {@code DELETE}, {@code RESTORE}, an accept of another uploader's copy, {@code ADMIN_DELETE}, and a baseline of the
 * submission deleted.</p>
 *
 * <p><b>It never fails an upload item:</b> anything detection throws keeps the copy aside with cause
 * {@code DETECTION_ERROR}, once the upload checks pass; its reads run in a savepoint, so a database error leaves the
 * transaction usable. Only a failure to store the conflict fails the item.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConflictDetection {

    private static final Set<String> SERVER_SIDE_COMMANDS =
        Set.of("EDIT", "MOVE", "SPLIT", "DELETE", "RESTORE", "ADMIN_DELETE");
    private static final Set<String> UPLOAD_COMMANDS = Set.of("UPLOAD", "LEGACY_UPLOAD");
    private static final String SAVEPOINT = "conflict_detection";

    private final ActivationSwitches switches;
    private final SubmissionRevisionReader journal;
    private final ConflictStore conflicts;
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    /**
     * What a phone copy is to its stored submission, read before the upload checks, under the submission lock.
     *
     * @param received       the copy as received; built only when detection runs
     * @param stored         the stored submission, as the submission lock returned it, or null for a new one
     * @param template       the copy's form version, by which the legacy route would mint row ids
     * @param uploaderUserId the uploader's user id ({@code app_user.id}), by which the journal names them
     * @param uploader       the uploader's access snapshot, whose login says whether they created the submission
     */
    public ConflictCheck check(Supplier<PhoneCopy> received, DataSubmission stored, TemplateVersionContext template,
                               String uploaderUserId, CurrentUserDetails uploader) {
        if (stored == null || !switches.isOn(ConflictDetectionSwitch.CONFLICT_DETECTION)) {
            return ConflictCheck.OFF;
        }
        // The copy is what a conflict stores: a copy that cannot be built could not be stored either.
        PhoneCopy copy = received.get();
        Context context = new Context(copy, stored, uploaderUserId);
        Boolean rowsLackIds = null;
        jdbc.getJdbcTemplate().execute("SAVEPOINT " + SAVEPOINT);
        try {
            rowsLackIds = new MigrationRepeatIdGenerator(template).hasRowsWithoutIds(copy.canonical().document());
            ConflictCheck check = analyse(context, rowsLackIds, StoredSubmissionGuard.madeBy(stored, uploader));
            jdbc.getJdbcTemplate().execute("RELEASE SAVEPOINT " + SAVEPOINT);
            return check;
        } catch (RuntimeException failure) {
            jdbc.getJdbcTemplate().execute("ROLLBACK TO SAVEPOINT " + SAVEPOINT);
            jdbc.getJdbcTemplate().execute("RELEASE SAVEPOINT " + SAVEPOINT);
            // Identifiers and shapes only: never a value or an exception's message.
            log.error("Conflict detection failed for submission {} (form {}) from user {}: {}",
                stored.getUid(), stored.getForm(), uploaderUserId, FailureShape.of(failure));
            return ConflictCheck.failed(context, this, rowsLackIds == null || rowsLackIds);
        }
    }

    /** The copy, its submission and its uploader: what a check stores a conflict for. */
    record Context(PhoneCopy copy, DataSubmission stored, String uploaderUserId) {
    }

    private ConflictCheck analyse(Context context, boolean rowsLackIds, boolean creator) {
        String submissionId = context.stored().getId();
        String uploader = context.uploaderUserId();
        List<SubmissionRevisionReader.Revision> revisions = journal.revisions(submissionId);
        if (revisions.isEmpty()) {
            throw new IllegalStateException("The journal records no revision of submission " + submissionId);
        }
        Set<Long> plainAccepts = conflicts.plainlyAcceptedVersions(submissionId, uploader);
        Long base = null;
        for (SubmissionRevisionReader.Revision revision : revisions) {
            if (isBaseOf(revision, uploader, creator, plainAccepts)) {
                base = revision.version();
            }
        }
        long first = revisions.get(0).version();
        long after = base != null ? base : first;
        boolean serverSideChange = revisions.stream()
            .anyMatch(revision -> revision.version() > after && isServerSideChange(revision, uploader));
        boolean moved = revisions.stream()
            .anyMatch(revision -> revision.version() > after && "MOVE".equals(revision.command()));

        ComparedState copy = context.copy().canonical();
        boolean equalsLastDecided = conflicts.lastDecidedCopy(submissionId, uploader)
            .map(text -> sameState(context.copy().asReceived(), ComparedState.ofUploadItem(ComparedState.parse(text))))
            .orElse(false);
        boolean equalsBase = base != null && sameState(copy, ComparedState.of(journal.stateAt(submissionId, base)));
        boolean equalsCurrent = sameState(copy, ComparedState.of(journal.current(submissionId)));
        return ConflictCheck.analysed(this, context, rowsLackIds, new ConflictCheck.Analysis(
            base != null, after, equalsBase || equalsLastDecided, equalsCurrent, serverSideChange, moved));
    }

    /** The uploader's own revisions: each leaves the state equal to what they last sent. */
    private static boolean isBaseOf(SubmissionRevisionReader.Revision revision, String uploader, boolean creator,
                                    Set<Long> plainAccepts) {
        String command = revision.command();
        return UPLOAD_COMMANDS.contains(command) && uploader.equals(revision.actorUserId())
            || "HELD_RELEASE".equals(command) && uploader.equals(revision.onBehalfOfUserId())
            || "CONFLICT_ACCEPT".equals(command) && uploader.equals(revision.onBehalfOfUserId())
                && plainAccepts.contains(revision.version())
            || "BASELINE".equals(revision.kind()) && !revision.deleted() && creator;
    }

    private static boolean isServerSideChange(SubmissionRevisionReader.Revision revision, String uploader) {
        String command = revision.command();
        return SERVER_SIDE_COMMANDS.contains(command)
            || "CONFLICT_ACCEPT".equals(command) && !uploader.equals(revision.onBehalfOfUserId())
            || "BASELINE".equals(revision.kind()) && revision.deleted();
    }

    /**
     * Keeps the copy aside: opens the uploader's conflict on the submission, or, while one is open, counts the same
     * copy again or lets a different one replace it. Runs in the upload item's transaction; a failure here is the one
     * that fails the item.
     */
    void keepAside(Context context, ConflictCause cause, Long baseVersion, boolean rowsLackIds) {
        DataSubmission stored = context.stored();
        String uploader = context.uploaderUserId();
        PhoneCopy copy = context.copy();
        String copyText = text(copy);
        Optional<ConflictStore.OpenConflict> open = conflicts.findOpen(stored.getId(), uploader);
        if (open.isEmpty()) {
            String id = conflicts.open(new ConflictStore.NewConflict(stored.getId(), uploader, cause, baseVersion,
                rowsLackIds, copy.route(), copyText));
            log.info("Submission {} (form {}) from user {}: phone copy kept aside as conflict {} ({}, base version {})",
                stored.getUid(), stored.getForm(), uploader, id, cause, baseVersion);
        } else if (sameAsKept(copy, open.get())) {
            conflicts.sentAgain(open.get().id());
            log.debug("Submission {} (form {}) from user {}: the copy of conflict {} sent again",
                stored.getUid(), stored.getForm(), uploader, open.get().id());
        } else {
            conflicts.replace(open.get().id(), cause, baseVersion, rowsLackIds, copy.route(), copyText);
            log.info("Submission {} (form {}) from user {}: a new phone copy replaces that of conflict {} ({})",
                stored.getUid(), stored.getForm(), uploader, open.get().id(), cause);
        }
    }

    /** Whether the copy equals the one the open conflict keeps; a kept copy that cannot be read is different. */
    private static boolean sameAsKept(PhoneCopy copy, ConflictStore.OpenConflict open) {
        try {
            return sameState(copy.asReceived(), ComparedState.ofUploadItem(ComparedState.parse(open.phoneCopy())));
        } catch (RuntimeException unreadable) {
            return false;
        }
    }

    private String text(PhoneCopy copy) {
        try {
            return objectMapper.writeValueAsString(copy.uploadItem());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("A phone copy could not be written as JSON", e);
        }
    }
}
