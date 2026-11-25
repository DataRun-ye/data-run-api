package org.nmcpye.datarun.web.v2.idempotency;

import org.nmcpye.datarun.web.v2.caller.ApiCaller;
import org.nmcpye.datarun.web.v2.problem.ApiProblemException;

/**
 * What a command sees inside its transaction, which exists only after its idempotency key is claimed: the caller,
 * and the request's {@code If-Match} to compare with the version of the row it has just locked.
 */
public interface CommandContext {

    ApiCaller caller();

    /**
     * Compares the request's {@code If-Match} with the resource's current version. Call it once the resource's row
     * is locked (its first read in the transaction is {@code SELECT ... FOR UPDATE}, or {@code FOR NO KEY UPDATE}; see
     * {@link Command}) and before changing anything.
     *
     * @param currentVersion the locked row's version
     * @param versionHistory a link to the resource's versions for the 412, or {@code null}
     * @throws ApiProblemException 412 {@code precondition-failed}; the transaction then rolls back and the key is
     *                             free again
     */
    void checkVersion(long currentVersion, String versionHistory);
}
