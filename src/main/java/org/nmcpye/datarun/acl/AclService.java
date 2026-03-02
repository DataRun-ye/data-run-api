package org.nmcpye.datarun.acl;

import org.nmcpye.datarun.common.IdentifiableObject;
import org.nmcpye.datarun.security.CurrentUserDetails;

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
 * @author Hamza Assada
 * @since 16/05/2025
 */
public interface AclService {

    boolean canRead(IdentifiableObject<?> object, CurrentUserDetails userDetails);

    boolean hasMinimalRights(CurrentUserDetails userDetails);

    boolean canWrite(IdentifiableObject<?> object, CurrentUserDetails userDetails);

    boolean canUpdate(IdentifiableObject<?> object, CurrentUserDetails userDetails);

    boolean canAddNew(IdentifiableObject<?> object, CurrentUserDetails userDetails);

    boolean canDelete(IdentifiableObject<?> object, CurrentUserDetails userDetails);
}
