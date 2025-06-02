package org.nmcpye.datarun.acl;

/**
 * <pre><b>Bootstrap Existing State into ACL Tables:</b></pre>
 * A one-off service that scans database and for each `Assignment`, `Team`, and `FormTemplate`
 * <blockquote>
 * <pre> 1. Compute who should have which bits based on old methods (`getUserTeamsUIDs()`, `getFormAccess()`, etc.). </pre>
 * <pre> 2. Create or load a `MutableAcl` via `aclService.readAclById(oid)` (or `createAcl` if missing). </pre>
 * <pre> 3. Insert ACEs for each `PrincipalSid` and `GrantedAuthoritySid` with the appropriate `Permission` bits. </pre>
 * <pre> 4. Call `aclService.updateAcl(acl)`. </pre>
 * </blockquote>
 *
 * @author Hamza Assada (16-05-2025), <7amza.it@gmail.com>
 */
public class LegacyAclMigrationService {
    // ## 3. Bootstrap Existing State into ACL Tables
}
