package org.nmcpye.datarun.common.security;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Set;

/**
 * @author Hamza Assada, 24/04/2025
 */
@Value
@Builder
public class UserFormAccess {
    String user;
    String team;
    String form;
    Set<FormPermission> permissions;
    Instant validFrom;
    Instant validTo;
}

