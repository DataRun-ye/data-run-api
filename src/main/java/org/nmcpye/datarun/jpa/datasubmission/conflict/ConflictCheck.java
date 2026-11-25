package org.nmcpye.datarun.jpa.datasubmission.conflict;

import org.nmcpye.datarun.common.feedback.ErrorCode;

import java.util.Set;

/**
 * What conflict detection found of one phone copy, and what the upload does with it, in the spec's order:
 * <ol>
 *   <li>an unchanged re-send (a copy equal to its base version's state, or to the uploader's last decided conflict
 *       copy) changes nothing;</li>
 *   <li>a copy equal to the current state changes nothing;</li>
 *   <li>after a server-side {@code MOVE} later than the base version, a copy the upload checks refuse for access
 *       (E4124, E4114 or E4122) is kept aside as a conflict ({@link #keptAsideOnRefusal});</li>
 *   <li>a server-side change after the base version keeps the copy aside as a conflict;</li>
 *   <li>otherwise the upload applies as today.</li>
 * </ol>
 * For an uploader with a base version of their own, the first two come before the upload checks
 * ({@link #changesNothing}), so a copy that would now be refused is still answered {@code updated}. For any other
 * uploader the upload checks run first, exactly as before, and every refusal stands. An authorized copy then changes
 * nothing when it equals the current state or their last decided conflict copy; otherwise it takes the submission's
 * first revision as its base, so a server-side change since then keeps it aside, and without one it applies as today
 * ({@link #afterChecks}). Whatever it decides, the phone is answered {@code updated}.
 */
public final class ConflictCheck {

    /** The access refusals a move can cause: the stored submission's, the copy's team, and the copy's form grant. */
    private static final Set<ErrorCode> MOVE_REFUSALS = Set.of(ErrorCode.E4124, ErrorCode.E4114, ErrorCode.E4122);

    /** What the upload does once the upload checks have passed. */
    public enum Outcome {
        /** Saves the copy, as before detection existed. */
        APPLY,
        /** Writes nothing: the copy changes nothing. */
        NOTHING,
        /** Writes nothing: the copy is kept aside, and its conflict is stored. */
        KEPT_ASIDE
    }

    /** Detection does not run: switched off, a new submission, or a practice upload or a release. */
    public static final ConflictCheck OFF = new ConflictCheck(null, null, false, null);

    /**
     * What detection read before the upload checks.
     *
     * @param ownBase          the uploader has a base version of their own
     * @param base             their base version, or else the submission's first revision
     * @param unchangedResend  the copy equals their own base version's state, or their last decided conflict copy
     * @param equalsCurrent    the copy equals the current state
     * @param serverSideChange a server-side change came after the base
     * @param moved            a server-side {@code MOVE} came after the base
     */
    record Analysis(boolean ownBase, long base, boolean unchangedResend, boolean equalsCurrent,
                    boolean serverSideChange, boolean moved) {
    }

    private final ConflictDetection detection;
    private final ConflictDetection.Context context;
    private final boolean rowsLackIds;
    /** Null when detection failed. */
    private final Analysis analysis;

    private ConflictCheck(ConflictDetection detection, ConflictDetection.Context context, boolean rowsLackIds,
                          Analysis analysis) {
        this.detection = detection;
        this.context = context;
        this.rowsLackIds = rowsLackIds;
        this.analysis = analysis;
    }

    static ConflictCheck analysed(ConflictDetection detection, ConflictDetection.Context context, boolean rowsLackIds,
                                  Analysis analysis) {
        return new ConflictCheck(detection, context, rowsLackIds, analysis);
    }

    /**
     * Detection failed: the copy is kept aside once the upload checks pass.
     *
     * @param rowsLackIds whether the copy's rows lack ids; true when tagging itself failed, the stricter of the two
     *                    for a later accept
     */
    static ConflictCheck failed(ConflictDetection.Context context, ConflictDetection detection, boolean rowsLackIds) {
        return new ConflictCheck(detection, context, rowsLackIds, null);
    }

    /**
     * Before the upload checks: whether the copy changes nothing, so the upload writes nothing and skips the checks.
     * Only for an uploader with a base version of their own.
     */
    public boolean changesNothing() {
        return analysis != null && analysis.ownBase() && (analysis.unchangedResend() || analysis.equalsCurrent());
    }

    /**
     * When the upload checks refuse the copy: whether it is kept aside instead. Only for an uploader with a base
     * version of their own, after a server-side {@code MOVE} later than it, and only for the access refusals a move
     * causes (E4124, E4114, E4122); any other refusal, and every refusal when detection failed, stays a refusal.
     * Keeping it aside stores its conflict here, in the upload item's transaction.
     */
    public boolean keptAsideOnRefusal(ErrorCode refusal) {
        if (analysis == null || !analysis.ownBase() || !analysis.moved() || !MOVE_REFUSALS.contains(refusal)) {
            return false;
        }
        detection.keepAside(context, ConflictCause.SERVER_SIDE_CHANGE, analysis.base(), rowsLackIds);
        return true;
    }

    /**
     * Once the upload checks have passed: whether to apply the copy, write nothing, or keep it aside. Keeping it aside
     * stores its conflict here, in the upload item's transaction.
     */
    public Outcome afterChecks() {
        if (context == null) {
            return Outcome.APPLY;
        }
        if (analysis == null) {
            detection.keepAside(context, ConflictCause.DETECTION_ERROR, null, rowsLackIds);
            return Outcome.KEPT_ASIDE;
        }
        if (!analysis.ownBase() && (analysis.unchangedResend() || analysis.equalsCurrent())) {
            return Outcome.NOTHING;
        }
        if (analysis.serverSideChange()) {
            detection.keepAside(context, ConflictCause.SERVER_SIDE_CHANGE, analysis.base(), rowsLackIds);
            return Outcome.KEPT_ASIDE;
        }
        return Outcome.APPLY;
    }
}
