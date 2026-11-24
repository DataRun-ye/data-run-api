package org.nmcpye.datarun.jpa.datasubmission.command;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What a release or discard did, per uid. {@code changed}: decided or sent
 * again by someone else meanwhile, so left as it was; look again.
 * {@code remaining}: matching uploads left for another call.
 */
public record HeldDecisionResult(
    List<String> released,
    List<String> discarded,
    List<String> superseded,
    List<String> changed,
    Map<String, String> failed,
    long remaining) {

    static HeldDecisionResult empty(long remaining) {
        return new HeldDecisionResult(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
            new LinkedHashMap<>(), remaining);
    }
}
