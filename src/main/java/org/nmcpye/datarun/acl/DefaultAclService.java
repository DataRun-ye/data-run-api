package org.nmcpye.datarun.acl;

import org.nmcpye.datarun.common.AuditableObject;
import org.nmcpye.datarun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.useraccess.dataform.FormAccessService;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
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
 * @author Hamza Assada, <7amza.it@gmail.com> <16-05-2025>
 */
@Service
public class DefaultAclService implements AclService {
    private final MutableAclService aclService;

    private final FormAccessService formAccessService;

    public DefaultAclService(MutableAclService aclService, FormAccessService formAccessService) {
        this.aclService = aclService;
        this.formAccessService = formAccessService;
    }

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
    @Override
    public void assignPermissions(Object domainObject, Sid sid, Permission... permissions) {
        ObjectIdentity oid = new ObjectIdentityImpl(domainObject.getClass(),
            ((AuditableObject<?>) domainObject).getUid());
        MutableAcl acl;
        try {
            acl = (MutableAcl) aclService.readAclById(oid);
        } catch (NotFoundException nfe) {
            acl = aclService.createAcl(oid);
        }

        for (Permission permission : permissions) {
            acl.insertAce(acl.getEntries().size(), permission, sid, true);
        }

        aclService.updateAcl(acl);
    }

    @Override
    public void createChildAcl(ObjectIdentity childOid, ObjectIdentity parentOid,
                               Sid sid, Permission perm) {
        // read or create parent ACL
        MutableAcl parentAcl = (MutableAcl) aclService.readAclById(parentOid);
        // create child ACL
        MutableAcl childAcl = aclService.createAcl(childOid);
        // link and turn on inheritance
        childAcl.setParent(parentAcl);
        childAcl.setEntriesInheriting(true);
        // grant, e.g., READ to a user
        childAcl.insertAce(childAcl.getEntries().size(), perm, sid, true);
        aclService.updateAcl(childAcl);
    }

    @Override
    public Set<Permission> getEffectiveDataPermissions(ObjectIdentity oid) {
        // readAclById will traverse parent links if entriesInheriting==true
        Acl acl = aclService.readAclById(oid);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Sid userSid = new PrincipalSid(auth);
        // check each known permission
        Set<Permission> granted = new HashSet<>();
        for (Permission p : DataPermission.PERMISSIONS) {
            if (acl.isGranted(Collections.singletonList(p),
                Collections.singletonList(userSid), false)) {
                granted.add(p);
            }
        }
        return granted;
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
        if (userDetails.isSuper()) return true;
        return !userDetails.getUserTeamsUIDs().isEmpty() || !userDetails.getUserFormsUIDs().isEmpty();
    }

    @Override
    public boolean canWrite(AuditableObject<?> object, CurrentUserDetails userDetails) {
        if (!hasMinimalRights(userDetails)) return false;
        if (object instanceof DataFormSubmission submission) {
            return formAccessService.canSubmitData(submission.getForm());
        }
        return false;
    }

    @Override
    public boolean canUpdate(AuditableObject<?> object, CurrentUserDetails userDetails) {
        if (!hasMinimalRights(userDetails)) return false;
        if (object instanceof DataFormSubmission submission) {
            return formAccessService.canEditSubmissions(submission.getForm());
        }
        return false;
    }

    @Override
    public boolean canAddNew(AuditableObject<?> object, CurrentUserDetails userDetails) {
        if (!hasMinimalRights(userDetails)) return false;
        if (object instanceof DataFormSubmission submission) {
            return formAccessService.canAddSubmissions(submission.getForm());
        }
        return false;
    }

    @Override
    public boolean canDelete(AuditableObject<?> object, CurrentUserDetails userDetails) {
        if (!hasMinimalRights(userDetails)) return false;
        if (object instanceof DataFormSubmission submission) {
            return formAccessService.canDeleteSubmissions(submission.getForm());
        }
        return false;
    }
}
