package org.nmcpye.datarun.acl;

import org.nmcpye.datarun.common.AuditableObject;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.datatemplateprocessor.FormAccessService;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.springframework.stereotype.Service;

/**
 * @author Hamza Assada
 * @since 16/05/2025
 */
@Service
public class DefaultAclService implements AclService {
    private final FormAccessService formAccessService;

    public DefaultAclService(FormAccessService formAccessService) {
        this.formAccessService = formAccessService;
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
        if (object instanceof DataSubmission submission) {
            return formAccessService.canSubmitData(submission.getForm());
        }
        return false;
    }

    @Override
    public boolean canUpdate(AuditableObject<?> object, CurrentUserDetails userDetails) {
        if (userDetails.isSuper()) return true;
        if (object instanceof DataSubmission submission) {
            return formAccessService.canEditSubmissions(submission.getForm());
        }
        return false;
    }

    @Override
    public boolean canAddNew(AuditableObject<?> object, CurrentUserDetails userDetails) {
        if (userDetails.isSuper()) return true;
        if (object instanceof DataSubmission submission) {
            return formAccessService.canAddSubmissions(submission.getForm());
        }
        return false;
    }

    @Override
    public boolean canDelete(AuditableObject<?> object, CurrentUserDetails userDetails) {
        if (userDetails.isSuper()) return true;
        if (object instanceof DataSubmission submission) {
            return formAccessService.canDeleteSubmissions(submission.getForm());
        }
        return false;
    }
}
