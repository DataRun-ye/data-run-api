package org.nmcpye.datarun.schema;

import java.util.List;

/**
 * @author Hamza Assada 01/05/2025 (7amza.it@gmail.com)
 */
public interface ImportableEntityHandler<T, K> {
    /**
     * The entity’s Java type.
     */
    Class<T> getEntityType();

    /**
     * The identifier type (e.g. Long, UUID, String code).
     */
    Class<K> getKeyType();

    /**
     * Dry-run validation: parse inputs → entities → return all issues.
     */
    List<ImportIssue> validate(List<T> items, ImportContext ctx);

    /**
     * Persist (upsert) on real run.
     * Should be transactional at handler-scope.
     */
    void persist(List<T> items, ImportContext ctx);
}
