package org.nmcpye.datarun.acl;

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
}
