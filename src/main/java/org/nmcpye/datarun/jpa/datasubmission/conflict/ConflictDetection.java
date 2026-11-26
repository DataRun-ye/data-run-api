package org.nmcpye.datarun.jpa.datasubmission.conflict;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.common.exceptions.FailureShape;
import org.nmcpye.datarun.config.activation.ActivationSwitches;
import org.nmcpye.datarun.jpa.datasubmission.revision.SubmissionRevisionReader;
import org.nmcpye.datarun.jpa.datasubmission.revision.SubmissionRevisionReader.Revision;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.LongFunction;

import static org.nmcpye.datarun.jpa.datasubmission.conflict.ConflictComparison.sameState;

/**
 * Conflict detection (spec, Conflicts), behind its activation switch ({@link ConflictDetectionSwitch}). For each
 * upload item of a stored submission, under the submission lock and before the upload checks, it finds the uploader's
 * base version in the journal and compares the phone copy; the upload then follows the {@link ConflictCheck} it
 * returns. Off, or for a new submission, the upload does not ask, and runs exactly as before.
 *
 * <p><b>What it reads:</b> plain values the upload builds from what it holds under the submission lock (the stored
 * state, the copy with its tag, the uploader's user id), the journal, and its own conflicts. Never the submission
 * entity, the uploader's access, a form version or the upload checks' codes: the upload keeps those.</p>
 *
 * <p><b>The base version</b> is the uploader's newest revision of the submission: their {@code UPLOAD} or
 * {@code LEGACY_UPLOAD}, a {@code HELD_RELEASE} on their behalf, a plain {@code CONFLICT_ACCEPT} of their copy, or a
 * baseline of the live submission when they are its creator ({@link StoredState#uploaderIsCreator}). An uploader with
 * none has no base version of their own.</p>
 *
 * <p><b>Server-side changes</b> are revisions their phone did not make: {@code EDIT}, {@code MOVE}, {@code SPLIT},
 * {@code DELETE}, {@code RESTORE}, an accept of another uploader's copy, {@code ADMIN_DELETE}, and a baseline of the
 * submission deleted, which counts for every uploader, also as the submission's first revision.</p>
 *
 * <p><b>A changed copy of a submission that is deleted now</b> is kept aside, whatever the base: under 6.5.6 a re-send
 * onto a deleted submission wrote an {@code UPLOAD} revision onto it, a base that hides the delete. An unchanged copy
 * still changes nothing, and an open conflict still comes first.</p>
 *
 * <p><b>An open conflict comes first:</b> while the uploader has one on the submission, it always holds their newest
 * copy. The same copy again only counts; any other copy, one equal to the base or to the current state included,
 * replaces it. So an uploader with an open conflict never writes the submission directly, and an accept never applies
 * a copy their phone abandoned.</p>
 *
 * <p><b>It never fails an upload item:</b> anything detection throws keeps the copy aside with cause
 * {@code DETECTION_ERROR}, once the upload checks pass; its reads run in a savepoint, so a database error leaves the
 * transaction usable. Only a failure to store the conflict fails the item. Its log lines name the submission by its id
 * and the uploader by their user id.</p>
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

    /** Whether detection runs: the upload builds what it reads only then. */
    public boolean isOn() {
        return switches.isOn(ConflictDetectionSwitch.CONFLICT_DETECTION);
    }

    /**
     * What a phone copy is to its stored submission, read before the upload checks, under the submission lock;
     * {@link ConflictCheck#OFF} when detection is switched off.
     *
     * @param copy           the copy as received, tagged before any row id was minted
     * @param stored         the stored submission's state, as the submission lock returned it
     * @param uploaderUserId the uploader's user id ({@code app_user.id}), by which the journal names them
     */
    public ConflictCheck check(PhoneCopy copy, StoredState stored, String uploaderUserId) {
        if (!isOn()) {
            return ConflictCheck.OFF;
        }
        Context context = new Context(copy, stored, uploaderUserId);
        jdbc.getJdbcTemplate().execute("SAVEPOINT " + SAVEPOINT);
        try {
            ConflictCheck check = ConflictCheck.analysed(this, context, analyse(context));
            jdbc.getJdbcTemplate().execute("RELEASE SAVEPOINT " + SAVEPOINT);
            return check;
        } catch (RuntimeException failure) {
            jdbc.getJdbcTemplate().execute("ROLLBACK TO SAVEPOINT " + SAVEPOINT);
            jdbc.getJdbcTemplate().execute("RELEASE SAVEPOINT " + SAVEPOINT);
            // Identifiers and shapes only: never a value or an exception's message.
            log.error("Conflict detection failed for submission {} from user {}: {}",
                stored.submissionId(), uploaderUserId, FailureShape.of(failure));
            return ConflictCheck.failed(context, this);
        }
    }

    /** The copy, its submission and its uploader: what a check stores a conflict for. */
    record Context(PhoneCopy copy, StoredState stored, String uploaderUserId) {
    }

    /**
     * What the decision reads of the journal and of the uploader's conflicts on the submission, as plain values.
     *
     * @param revisions       the submission's revisions, oldest first
     * @param plainAccepts    the versions plain accepts of the uploader's copies made
     * @param openCopy        the copy of the uploader's open conflict, as stored text, if one is open
     * @param lastDecidedCopy the copy of the uploader's conflict decided last, as stored text, if any
     */
    record Facts(List<Revision> revisions, Set<Long> plainAccepts, Optional<String> openCopy,
                 Optional<String> lastDecidedCopy) {
    }

    private ConflictCheck.Analysis analyse(Context context) {
        String submissionId = context.stored().submissionId();
        String uploader = context.uploaderUserId();
        Facts facts = new Facts(
            journal.revisions(submissionId),
            conflicts.plainlyAcceptedVersions(submissionId, uploader),
            conflicts.findOpen(submissionId, uploader).map(ConflictStore.OpenConflict::phoneCopy),
            conflicts.lastDecidedCopy(submissionId, uploader));
        return analysis(context, facts, version -> ComparedState.of(journal.stateAt(submissionId, version)));
    }

    /**
     * The decision's findings on plain values alone: the uploader's base version, the server-side changes and moves
     * after it, and what the copy equals.
     *
     * @param stateAt a revision's state, read only for the uploader's own base version
     */
    static ConflictCheck.Analysis analysis(Context context, Facts facts, LongFunction<ComparedState> stateAt) {
        List<Revision> revisions = facts.revisions();
        String submissionId = context.stored().submissionId();
        if (revisions.isEmpty()) {
            throw new IllegalStateException("The journal records no revision of submission " + submissionId);
        }
        String uploader = context.uploaderUserId();
        boolean creator = context.stored().uploaderIsCreator();
        Long base = null;
        for (Revision revision : revisions) {
            if (isBaseOf(revision, uploader, creator, facts.plainAccepts())) {
                base = revision.version();
            }
        }
        long first = revisions.get(0).version();
        long after = base != null ? base : first;
        // A deleted baseline is a server-side change for every uploader, even as the first revision, which is where
        // the comparison starts for an uploader without a base of their own: the submission was deleted before the
        // journal began.
        boolean noBase = base == null;
        boolean serverSideChange = revisions.stream().anyMatch(revision ->
            revision.version() > after && isServerSideChange(revision, uploader)
                || noBase && revision.version() == after && isDeletedBaseline(revision));
        boolean moved = revisions.stream()
            .anyMatch(revision -> revision.version() > after && "MOVE".equals(revision.command()));

        PhoneCopy copy = context.copy();
        ConflictCheck.OpenCopy openCopy = facts.openCopy()
            .map(text -> sameAsKept(copy, text) ? ConflictCheck.OpenCopy.SAME : ConflictCheck.OpenCopy.DIFFERENT)
            .orElse(ConflictCheck.OpenCopy.NONE);
        boolean equalsLastDecided = facts.lastDecidedCopy()
            .map(text -> ConflictComparison.sameUploadItem(copy.uploadItem(), ComparedState.parse(text)))
            .orElse(false);
        boolean equalsBase = base != null && sameState(copy.canonical(), stateAt.apply(base));
        boolean equalsCurrent = sameState(copy.canonical(), context.stored().compared());
        return new ConflictCheck.Analysis(base != null, after, equalsBase || equalsLastDecided, equalsCurrent,
            serverSideChange, moved, openCopy, context.stored().deleted());
    }

    /** The uploader's own revisions: each leaves the state equal to what they last sent. */
    private static boolean isBaseOf(Revision revision, String uploader, boolean creator, Set<Long> plainAccepts) {
        String command = revision.command();
        return UPLOAD_COMMANDS.contains(command) && uploader.equals(revision.actorUserId())
            || "HELD_RELEASE".equals(command) && uploader.equals(revision.onBehalfOfUserId())
            || "CONFLICT_ACCEPT".equals(command) && uploader.equals(revision.onBehalfOfUserId())
                && plainAccepts.contains(revision.version())
            || "BASELINE".equals(revision.kind()) && !revision.deleted() && creator;
    }

    private static boolean isServerSideChange(Revision revision, String uploader) {
        String command = revision.command();
        return SERVER_SIDE_COMMANDS.contains(command)
            || "CONFLICT_ACCEPT".equals(command) && !uploader.equals(revision.onBehalfOfUserId())
            || isDeletedBaseline(revision);
    }

    private static boolean isDeletedBaseline(Revision revision) {
        return "BASELINE".equals(revision.kind()) && revision.deleted();
    }

    /**
     * Keeps the copy aside: opens the uploader's conflict on the submission, or, while one is open, counts the same
     * copy again or lets a different one replace it. Runs in the upload item's transaction; a failure here is the one
     * that fails the item.
     *
     * @param cause why, or null when only the open conflict keeps the copy aside, whose cause then stays
     */
    void keepAside(Context context, ConflictCause cause, Long baseVersion) {
        String submissionId = context.stored().submissionId();
        String uploader = context.uploaderUserId();
        PhoneCopy copy = context.copy();
        String copyText = text(copy);
        Optional<ConflictStore.OpenConflict> open = conflicts.findOpen(submissionId, uploader);
        if (open.isEmpty()) {
            if (cause == null) {
                throw new IllegalStateException("A conflict is opened only for a cause");
            }
            String id = conflicts.open(new ConflictStore.NewConflict(submissionId, uploader, cause, baseVersion,
                copy.rowsLackIds(), copy.route(), copyText));
            log.info("Submission {} from user {}: phone copy kept aside as conflict {} ({}, base version {}, head {})",
                submissionId, uploader, id, cause, baseVersion, context.stored().version());
        } else if (sameAsKept(copy, open.get().phoneCopy())) {
            conflicts.sentAgain(open.get().id());
            log.debug("Submission {} from user {}: the copy of conflict {} sent again",
                submissionId, uploader, open.get().id());
        } else {
            conflicts.replace(open.get().id(), cause, baseVersion, copy.rowsLackIds(), copy.route(), copyText);
            log.info("Submission {} from user {}: a new phone copy replaces that of conflict {} ({})",
                submissionId, uploader, open.get().id(), cause);
        }
    }

    /** Whether the copy equals a kept one, as stored text; a kept copy that cannot be read is different. */
    private static boolean sameAsKept(PhoneCopy copy, String keptText) {
        try {
            return ConflictComparison.sameUploadItem(copy.uploadItem(), ComparedState.parse(keptText));
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
