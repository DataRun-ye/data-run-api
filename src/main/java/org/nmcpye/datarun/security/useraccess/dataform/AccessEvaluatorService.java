package org.nmcpye.datarun.security.useraccess.dataform;

import org.springframework.stereotype.Component;

/**
 * @author Hamza Assada, 24/04/2025
 */
@Component
public class AccessEvaluatorService {

    Boolean canView(String form) {
        return form.equals("view");
    }

    Boolean canSubmit(String form) {
        return false;
    }

    Boolean canApprove(String form) {
        return false;
    }
}
