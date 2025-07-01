package org.nmcpye.datarun.common;

/**
 * @author Hamza Assada 20/03/2025 (7amza.it@gmail.com)
 */
public interface VersionedObject<ID> extends AuditableObject<ID> {
    Integer getVersion();

    Integer setVersion(Integer deleted);
}
