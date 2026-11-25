package org.nmcpye.datarun.web.v2.idempotency;

import org.nmcpye.datarun.web.v2.ids.ApiIds;

/**
 * What a completed command did, as the idempotency record keeps it: the success status it answered and the
 * resource it left at a version. No value of the request or the response: a replay renders the resource at that
 * version again ({@link OutcomeRenderer}).
 *
 * @param responseStatus  the success status the command answered, 200 to 299
 * @param resourceType    the kind of resource, a short stable name (at most 32 characters)
 * @param resourceId      the resource's id, as v2 shows it ({@link ApiIds})
 * @param resourceVersion the resource's version after the command, its entity tag
 */
public record CommandOutcome(int responseStatus, String resourceType, String resourceId, long resourceVersion) {

    public CommandOutcome {
        if (responseStatus < 200 || responseStatus > 299) {
            throw new IllegalArgumentException("An outcome is a success status");
        }
        if (resourceType == null || resourceType.isBlank() || resourceType.length() > 32) {
            throw new IllegalArgumentException("A resource type is 1 to 32 characters");
        }
        if (!ApiIds.isWellFormed(resourceId)) {
            throw new IllegalArgumentException("A resource id is a v2 id");
        }
        if (resourceVersion < 0) {
            throw new IllegalArgumentException("A version is never negative");
        }
    }

    public static CommandOutcome ok(String resourceType, String resourceId, long resourceVersion) {
        return new CommandOutcome(200, resourceType, resourceId, resourceVersion);
    }
}
