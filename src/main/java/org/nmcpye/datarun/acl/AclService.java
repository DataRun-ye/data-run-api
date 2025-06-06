package org.nmcpye.datarun.acl;

import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;

import java.util.Set;

/**
 * <pre>
 *     {@code
 *  // in the service:
 * public DocumentDto fetchDocument(Long id) {
 *     Document doc = documentRepo.findById(id).orElseThrow(...);
 *     ObjectIdentity oid = new ObjectIdentityImpl(Document.class, id);
 *     Set<Permission> perms = getEffectiveDataPermissions(oid);
 *     return new DocumentDto(id, doc.getTitle(), perms);
 * }
 * }
 * </pre>
 *
 * @author Hamza Assada 16/05/2025 <7amza.it@gmail.com>
 */
public interface AclService {
    /**
     * Example:
     *
     * <pre>
     * {@code
     *  assignPermissions(
     *     activity,
     *     new PrincipalSid("alice"),
     *     MetaDataPermission.META_READ,
     *     MetaDataPermission.META_READ,
     * );
     *
     * // for a team or a rule
     * assignPermissions(
     *     formTemplate,
     *     // TeamSid, etc.
     *     new GrantedAuthoritySid("ROLE_EDITOR"),
     *     DataPermission.DATA_WRITE,
     *     DataPermission.DATA_UPDATE,
     *     true
     * );
     * }
     * </pre>
     * <p>
     * Checking Combined Permissions:
     * <pre>
     *     {@code
     *      // “Does the user have BOTH META_READ and META_WRITE?”
     *      Permission required = new CumulativePermission()
     *           .set(MetaDataPermission.META_READ)
     *           .set(MetaDataPermission.META_WRITE);
     *
     *      boolean ok = acl.isGranted(
     *          Collections.singletonList(required),
     *          Collections.singletonList(new PrincipalSid(auth)),
     *          false
     *      );
     *     }
     * </pre>
     * <p>
     * Or check just data-level:
     * <pre>
     *     {@code
     *        acl.isGranted(
     *        Collections.singletonList(DataPermission.DATA_ADMIN),
     *        Collections.singletonList(userSid),
     *        false
     *      );
     *      // for user detail properties
     *      Acl acl = aclService.readAclById(oid);
     *      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
     *      List<Sid> sids = SidRetrievalStrategy.buildSids(auth);
     *      boolean granted = acl.isGranted(
     *     Arrays.asList(DataPermission.DATA_READ),
     *     sids,
     *     false
     * );
     *   }
     * </pre>
     *
     * @param domainObject AuditableObject system entity
     * @param sid          who identity
     * @param permissions  the permissions to grant
     */
    void assignPermissions(Object domainObject, Sid sid, Permission... permissions);

    void createChildAcl(ObjectIdentity childOid, ObjectIdentity parentOid, Sid sid, Permission perm);

    Set<Permission> getEffectiveDataPermissions(ObjectIdentity oid);
}
