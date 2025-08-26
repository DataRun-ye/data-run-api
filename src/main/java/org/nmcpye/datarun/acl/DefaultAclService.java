package org.nmcpye.datarun.acl;

import org.nmcpye.datarun.common.AuditableObject;
import org.nmcpye.datarun.mongo.accessfilter.FormAccessService;
import org.nmcpye.datarun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.security.CurrentUserDetails;
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
 * @author Hamza Assada
 * @since 16/05/2025
 */
@Service
public class DefaultAclService implements AclService {
    private final MutableAclService aclService;
    private final MutableAclService lookupService;    // for reading ACLs
    private final SidRetrievalStrategy sidRetrieval;
    private final FormAccessService formAccessService;

    public DefaultAclService(MutableAclService mutableAclService,
                             MutableAclService lookupService,
                             SidRetrievalStrategy sidRetrieval, FormAccessService formAccessService) {
        this.aclService = mutableAclService;
        this.lookupService = lookupService;
        this.sidRetrieval = sidRetrieval;
        this.formAccessService = formAccessService;
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
        try {
            Method m = domainObject.getClass().getMethod("getUid");
            return (Serializable) m.invoke(domainObject);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot extract id", e);
        }
    }

    @Override
    public boolean canRead(AuditableObject<?> object, CurrentUserDetails userDetails) {
        if (userDetails == null) return false;
        if (userDetails.isSuper()) return true;
        if (userDetails.getUserTeamsUIDs().isEmpty() || userDetails.getUserTeamsUIDs().isEmpty()) return false;
        return false;
    }

    @Override
    public boolean hasMinimalRights(CurrentUserDetails userDetails) {
        return userDetails.isSuper() || !userDetails.getUserTeamsUIDs().isEmpty();
    }

    @Override
    public boolean canWrite(AuditableObject<?> object, CurrentUserDetails userDetails) {
        if (userDetails.isSuper()) return true;
        if (object instanceof DataFormSubmission submission) {
            return formAccessService.canSubmitData(submission.getForm());
        }
        return false;
    }

    @Override
    public boolean canUpdate(AuditableObject<?> object, CurrentUserDetails userDetails) {
        if (userDetails.isSuper()) return true;
        if (object instanceof DataFormSubmission submission) {
            return formAccessService.canEditSubmissions(submission.getForm());
        }
        return false;
    }

    @Override
    public boolean canAddNew(AuditableObject<?> object, CurrentUserDetails userDetails) {
        if (userDetails.isSuper()) return true;
        if (object instanceof DataFormSubmission submission) {
            return formAccessService.canAddSubmissions(submission.getForm());
        }
        return false;
    }

    @Override
    public boolean canDelete(AuditableObject<?> object, CurrentUserDetails userDetails) {
        if (userDetails.isSuper()) return true;
        if (object instanceof DataFormSubmission submission) {
            return formAccessService.canDeleteSubmissions(submission.getForm());
        }
        return false;
    }
}
