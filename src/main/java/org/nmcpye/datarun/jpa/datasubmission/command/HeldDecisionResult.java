package org.nmcpye.datarun.jpa.datasubmission.command;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What a release or discard did, per uid. {@code changed}: decided or sent
 * again by someone else meanwhile, so left as it was, with nothing saved;
 * look again. {@code remaining}: matching uploads left for another call.
 * {@code runAgain} says whether to call again; {@code stopReason} says why a
 * call that made no progress stopped.
 *
 * @param firstFailures the uploads in {@code failed} that no release had tried before; not part of the answer
 */
public record HeldDecisionResult(
    List<String> released,
    List<String> discarded,
    List<String> superseded,
    List<String> changed,
    Map<String, String> failed,
    long remaining,
    @JsonIgnore Set<String> firstFailures) {

    /**
     * Whether to make the same call again: it made progress, and uploads are left (another batch matches, or one
     * changed while it ran). A loop that calls while this is true ends: each call decides an upload, or meets one that
     * no release had tried before, and both run out. Uploads in {@code failed} stay held until their cause is fixed
     * or they are discarded.
     */
    @JsonProperty("runAgain")
    public boolean runAgain() {
        return progress() && (remaining > 0 || !changed.isEmpty());
    }

    /**
     * Why a call that made no progress stopped although uploads are left, or null. Running it again would repeat
     * it: look at those uploads first.
     */
    @JsonProperty("stopReason")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String stopReason() {
        if (progress() || (remaining == 0 && changed.isEmpty() && failed.isEmpty())) {
            return null;
        }
        List<String> left = new ArrayList<>();
        if (!failed.isEmpty()) {
            left.add(failed.size() + " failed again");
        }
        if (!changed.isEmpty()) {
            left.add(changed.size() + " changed while it ran");
        }
        if (remaining > 0) {
            left.add(remaining + " more wait behind them");
        }
        return "No progress: " + String.join(", ", left) + ". Look at these uploads before running it again.";
    }

    /** Something was decided, or an upload was tried for the first time (it now waits behind the untried ones). */
    private boolean progress() {
        return !released.isEmpty() || !discarded.isEmpty() || !superseded.isEmpty() || !firstFailures.isEmpty();
    }

    static HeldDecisionResult empty(long remaining) {
        return new HeldDecisionResult(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
            new LinkedHashMap<>(), remaining, new LinkedHashSet<>());
    }
}
