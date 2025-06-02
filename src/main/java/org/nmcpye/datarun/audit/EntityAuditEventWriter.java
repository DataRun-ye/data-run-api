package org.nmcpye.datarun.audit;

import org.nmcpye.datarun.jpa.common.enumeration.EntityAuditAction;

@FunctionalInterface
public interface EntityAuditEventWriter {
    public void writeAuditEvent(Object target, EntityAuditAction action);
}
