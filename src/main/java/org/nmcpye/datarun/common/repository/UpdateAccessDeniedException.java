package org.nmcpye.datarun.common.repository;

import org.springframework.security.access.AccessDeniedException;

public class UpdateAccessDeniedException extends AccessDeniedException {

    public UpdateAccessDeniedException(String msg) {
        super(msg);
    }
}
