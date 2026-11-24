package org.nmcpye.datarun.jpa.datasubmission.command;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What a release or discard did, per uid. {@code changed}: decided or sent
 * again by someone else meanwhile, so left as it was, with nothing saved;
 * look again. {@code remaining}: matching uploads left for another call.
 * {@code runAgain} says whether either is left.
 */
public record HeldDecisionResult(
    List<String> released,
    List<String> discarded,
    List<String> superseded,
    List<String> changed,
    Map<String, String> failed,
    long remaining) {

    /**
     * Whether the same call has to be made again: an upload changed while it ran (a re-send refreshed it, or another
     * decision came first; for a release nothing was saved and it is still held), or matching uploads remain for
     * another batch. Uploads in {@code failed} stay held until their cause is fixed or they are discarded.
     */
    @JsonProperty("runAgain")
    public boolean runAgain() {
        return !changed.isEmpty() || remaining > 0;
    }

    static HeldDecisionResult empty(long remaining) {
        return new HeldDecisionResult(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
            new LinkedHashMap<>(), remaining);
    }
}
