package org.nmcpye.datarun.jpa.common;

/**
 * @author Hamza Assada 14/08/2025 (7amza.it@gmail.com)
 */
public interface Auditable {
    /**
     * entity id.
     * auditable entities should have a string ulid id.
     *
     * @return entity id
     */
    String getId();

    /**
     * subclasses should implement with @Version,
     * in the audit listener/writer, obtain lockVersion and
     * store it as commitVersion
     *
     * @return version number
     */
    Long getLockVersion();
}
