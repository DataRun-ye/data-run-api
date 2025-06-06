package org.nmcpye.datarun.common.repository;

import org.springframework.security.access.AccessDeniedException;

public class ReadAccessDeniedException extends AccessDeniedException {

    public ReadAccessDeniedException(String msg) {
        super(msg);
    }
}
