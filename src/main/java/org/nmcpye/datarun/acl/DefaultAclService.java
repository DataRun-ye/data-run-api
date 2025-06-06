package org.nmcpye.datarun.acl;

import org.springframework.security.acls.domain.CumulativePermission;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.model.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Hamza Assada 16/05/2025 <7amza.it@gmail.com>
 */
@Service
public class DefaultAclService implements AclService {
    private final MutableAclService aclService;
    private final MutableAclService lookupService;    // for reading ACLs
    private final SidRetrievalStrategy sidRetrieval;

    public DefaultAclService(MutableAclService mutableAclService,
                             MutableAclService lookupService,
                             SidRetrievalStrategy sidRetrieval) {
        this.aclService = mutableAclService;
        this.lookupService = lookupService;
        this.sidRetrieval = sidRetrieval;
    }

    @Override
    public void assignPermissions(Object domainObject, Sid sid, Permission... permissions) {
        ObjectIdentity oid = new ObjectIdentityImpl(domainObject.getClass(), getId(domainObject));
        MutableAcl acl;
        try {
            acl = (MutableAcl) aclService.readAclById(oid);
        } catch (NotFoundException nfe) {
            acl = aclService.createAcl(oid);
        }

        // clear existing ACEs for this Sid?
        // acl.getEntries().removeIf(entry -> entry.getSid().equals(sid));
        CumulativePermission cumulative = new CumulativePermission();
        for (Permission p : permissions) {
            cumulative.set(p);
        }
        acl.insertAce(acl.getEntries().size(), cumulative, sid, true);
        aclService.updateAcl(acl);
    }

    @Override
    public void createChildAcl(ObjectIdentity childOid, ObjectIdentity parentOid,
                               Sid sid, Permission perm) {
        MutableAcl parent = (MutableAcl) aclService.readAclById(parentOid);
        MutableAcl child;
        try {
            child = (MutableAcl) aclService.readAclById(childOid);
        } catch (NotFoundException nfe) {
            child = aclService.createAcl(childOid);
        }
        child.setParent(parent);
        child.setEntriesInheriting(true);
        child.insertAce(child.getEntries().size(), perm, sid, true);
        aclService.updateAcl(child);
    }

    @Override
    public Set<Permission> getEffectiveDataPermissions(ObjectIdentity oid) {
        // readAclById will resolve inherited entries
        Acl acl = lookupService.readAclById(oid);
        List<Sid> sids = sidRetrieval.getSids(
            SecurityContextHolder.getContext().getAuthentication());
        return DataPermission.PERMISSIONS.stream()
            .filter(p -> {
                try {
                    return acl.isGranted(List.of(p), sids, false);
                } catch (NotFoundException | UnloadedSidException e) {
                    return false;
                }
            })
            .collect(Collectors.toSet());
    }

    private Serializable getId(Object domainObject) {
        // assume all your entities have getId()
        try {
            Method m = domainObject.getClass().getMethod("getUid");
            return (Serializable) m.invoke(domainObject);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot extract id", e);
        }
    }
}
