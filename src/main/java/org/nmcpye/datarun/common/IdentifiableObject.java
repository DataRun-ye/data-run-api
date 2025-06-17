package org.nmcpye.datarun.common;

/**
 * @author Hamza Assada 20/03/2025 <7amza.it@gmail.com>
 */
public interface IdentifiableObject<ID>
    extends AuditableObject<ID> {
    String getCode();

    String getName();
}
