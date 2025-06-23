package org.nmcpye.datarun.acl;

import org.nmcpye.datarun.common.AuditableObject;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;

import java.util.Set;

/**
 * @author Hamza Assada, <7amza.it@gmail.com> <16-05-2025>
 */
public interface AclService {
    void assignPermissions(Object domainObject, Sid sid, Permission... permissions);

    void createChildAcl(ObjectIdentity childOid, ObjectIdentity parentOid, Sid sid, Permission perm);

    Set<Permission> getEffectiveDataPermissions(ObjectIdentity oid);

    boolean canRead(AuditableObject<?> object, CurrentUserDetails userDetails);

    boolean hasMinimalRights(CurrentUserDetails userDetails);

    boolean canWrite(AuditableObject<?> object, CurrentUserDetails userDetails);

    boolean canUpdate(AuditableObject<?> object, CurrentUserDetails userDetails);

    boolean canAddNew(AuditableObject<?> object, CurrentUserDetails userDetails);

    boolean canDelete(AuditableObject<?> object, CurrentUserDetails userDetails);
}
