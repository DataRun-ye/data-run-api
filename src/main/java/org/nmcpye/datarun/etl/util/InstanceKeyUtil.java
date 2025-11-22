package org.nmcpye.datarun.etl.util;

import org.nmcpye.datarun.etl.model.TallCanonicalRow;

/**
 * Deterministic instance-key generation shared by upsert/delete logic.
 * <p>
 * Rules (kept exactly from your chosen logic):
 * - If repeatInstanceId present:
 * - if elementPath endsWith(']') -> key = repeatId + '|' + elementPath
 * - else -> key = repeatId
 * - else:
 * - key = submissionUid + '|' + elementPath
 * <p>
 * Also truncates to INSTANCE_KEY_MAX to protect DB column length.
 */
public final class InstanceKeyUtil {
    private InstanceKeyUtil() {
    }

    public static String computeInstanceKey(TallCanonicalRow r) {
        if (r == null) return "unknown|";
        return computeInstanceKey(r.getRepeatInstanceId(), r.getElementPath(), r.getSubmissionUid());
    }

    public static String computeInstanceKey(String repeatInstanceId, String elementPath, String submissionUid) {
        String element = elementPath == null ? "" : elementPath;
        String submission = (submissionUid == null || submissionUid.isBlank()) ? "unknown" : submissionUid;

        final String raw;
        if (repeatInstanceId != null && !repeatInstanceId.isBlank()) {
            if (element.endsWith("]")) {
                raw = repeatInstanceId + "|" + element;
            } else {
                raw = repeatInstanceId;
            }
        } else {
            raw = submission + "|" + element;
        }

        return raw;
    }
}
