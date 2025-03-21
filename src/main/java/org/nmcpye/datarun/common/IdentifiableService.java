package org.nmcpye.datarun.common;

/**
 * @author Hamza, 20/03/2025
 */
public interface IdentifiableService<T extends IdentifiableObject<ID>, ID>
    extends AuditableObjectService<T, ID> {
}
