package org.nmcpye.datarun.common.repository;

import org.springframework.security.access.AccessDeniedException;

public class CreateAccessDeniedException extends AccessDeniedException {

    public CreateAccessDeniedException(String msg) {
        super(msg);
    }
}
