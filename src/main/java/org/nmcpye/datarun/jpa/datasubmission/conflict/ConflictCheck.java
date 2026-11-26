package org.nmcpye.datarun.jpa.datasubmission.conflict;

/**
 * What conflict detection found of one phone copy, and what the upload does with it, in the spec's order:
 * <ol>
 *   <li>an unchanged re-send (a copy equal to its base version's state, or to the uploader's last decided conflict
 *       copy) changes nothing;</li>
 *   <li>a copy equal to the current state changes nothing;</li>
 *   <li>after a server-side {@code MOVE} later than the base version, a copy the upload checks refuse for access
 *       ({@link AccessRefusal}) is kept aside as a conflict ({@link #keepsAsideOnRefusal});</li>
 *   <li>a server-side change after the base version keeps the copy aside as a conflict, and so does any changed copy
 *       of a submission that is currently deleted, whatever the base;</li>
 *   <li>otherwise the upload applies as today.</li>
 * </ol>
 * While the uploader has an open conflict on the submission, it comes first: the same copy again only counts, and any
 * other copy replaces the kept one, once the checks pass, whatever these steps would say ({@link OpenCopy}).
 * For an uploader with a base version of their own, the first two come before the upload checks
 * ({@link #answeredBeforeChecks}), so a copy that would now be refused is still answered {@code updated}. For any other
 * uploader the upload checks run first, exactly as before, and every refusal stands. An authorized copy then changes
 * nothing when it equals the current state or their last decided conflict copy; otherwise it takes the submission's
 * first revision as its base, so a server-side change since then keeps it aside, and without one it applies as today
 * ({@link #afterChecks}). Whatever it decides, the phone is answered {@code updated}.
 *
 * <p>The decisions store nothing: the upload runs its own remaining checks on a copy it will not save (the Reference
 * checks, which create no entry then), and only then stores the conflict ({@link #keepAside}).</p>
 */
public final class ConflictCheck {

    /** The uploader's open conflict on the submission, as the copy finds it. */
    enum OpenCopy {
        /** No conflict of theirs is open. */
        NONE,
        /** The copy equals the open conflict's: it only counts, before the checks. */
        SAME,
        /** Another copy: it replaces the open conflict's, once the checks pass. */
        DIFFERENT
    }

    /** What the upload does once the upload checks have passed. */
    public enum Outcome {
        /** Saves the copy, as before detection existed. */
        APPLY,
        /** Writes nothing: the copy changes nothing. */
        NOTHING,
        /** Writes nothing to the submission: the copy is to be kept aside ({@link #keepAside}). */
        KEEP_ASIDE
    }

    /** Detection does not run: switched off, a new submission, or a practice upload or a release. */
    public static final ConflictCheck OFF = new ConflictCheck(null, null, null);

    /**
     * What detection read before the upload checks.
     *
     * @param ownBase          the uploader has a base version of their own
     * @param base             their base version, or else the submission's first revision
     * @param unchangedResend  the copy equals their own base version's state, or their last decided conflict copy
     * @param equalsCurrent    the copy equals the current state
     * @param serverSideChange a server-side change came after the base
     * @param moved            a server-side {@code MOVE} came after the base
     * @param open             the uploader's open conflict, as the copy finds it
     * @param deletedNow       the submission is deleted now
     */
    record Analysis(boolean ownBase, long base, boolean unchangedResend, boolean equalsCurrent,
                    boolean serverSideChange, boolean moved, OpenCopy open, boolean deletedNow) {

        /** What keeps a changed copy aside: a server-side change after the base, or the submission deleted now. */
        boolean keepsAside() {
            return serverSideChange || deletedNow;
        }
    }

    private final ConflictDetection detection;
    private final ConflictDetection.Context context;
    /** Null when detection failed. */
    private final Analysis analysis;

    private ConflictCheck(ConflictDetection detection, ConflictDetection.Context context, Analysis analysis) {
        this.detection = detection;
        this.context = context;
        this.analysis = analysis;
    }

    static ConflictCheck analysed(ConflictDetection detection, ConflictDetection.Context context, Analysis analysis) {
        return new ConflictCheck(detection, context, analysis);
    }

    /** Detection failed: the copy is kept aside once the upload checks pass. */
    static ConflictCheck failed(ConflictDetection.Context context, ConflictDetection detection) {
        return new ConflictCheck(detection, context, null);
    }

    /**
     * Before the upload checks: whether the item is answered now, the checks skipped and nothing written to the
     * submission. So is a copy equal to the uploader's open conflict's, which only counts there (stored here); and,
     * with no conflict of theirs open, an uploader's unchanged re-send or copy equal to the current state, when they
     * have a base version of their own.
     */
    public boolean answeredBeforeChecks() {
        if (analysis == null) {
            return false;
        }
        if (analysis.open() == OpenCopy.SAME) {
            keepAside();
            return true;
        }
        return analysis.open() == OpenCopy.NONE && analysis.ownBase()
            && (analysis.unchangedResend() || analysis.equalsCurrent());
    }

    /**
     * When the upload checks refuse the copy for access: whether it is to be kept aside instead. Only for an uploader
     * with a base version of their own, after a server-side {@code MOVE} later than it, and only for the access
     * refusals a move causes, as the upload names them; any other refusal (null), and every refusal when detection
     * failed, stays a refusal.
     */
    public boolean keepsAsideOnRefusal(AccessRefusal refusal) {
        return refusal != null && analysis != null && analysis.ownBase() && analysis.moved();
    }

    /** Once the upload checks have passed: whether to apply the copy, write nothing, or keep it aside. */
    public Outcome afterChecks() {
        if (context == null) {
            return Outcome.APPLY;
        }
        if (analysis == null || analysis.open() != OpenCopy.NONE) {
            return Outcome.KEEP_ASIDE;
        }
        if (!analysis.ownBase() && (analysis.unchangedResend() || analysis.equalsCurrent())) {
            return Outcome.NOTHING;
        }
        return analysis.keepsAside() ? Outcome.KEEP_ASIDE : Outcome.APPLY;
    }

    /**
     * Keeps the copy aside, once the upload's own checks are done: opens its conflict, or counts or replaces the open
     * one, in the upload item's transaction. Its cause is {@code DETECTION_ERROR} when detection failed.
     */
    public void keepAside() {
        if (context == null) {
            throw new IllegalStateException("Detection did not run: there is no copy to keep aside");
        }
        if (analysis == null) {
            detection.keepAside(context, ConflictCause.DETECTION_ERROR, null);
        } else {
            // Otherwise only the open conflict keeps the copy aside, and its cause stays.
            detection.keepAside(context, analysis.keepsAside() ? ConflictCause.SERVER_SIDE_CHANGE : null,
                analysis.base());
        }
    }
}
