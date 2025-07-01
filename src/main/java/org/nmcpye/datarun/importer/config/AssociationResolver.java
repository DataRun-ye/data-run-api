package org.nmcpye.datarun.importer.config;

/**
 * <pre>
 * A helper services to resolve entities by key. to make association
 * resolution reusable and avoid code duplication.
 * this service would use the appropriate repository and method based
 * on the keyField. <blockquote>
 * in this service we can also have a caching mechanism to avoid repeated
 * lookups for the same entity in one import (if multiple associations).
 * </blockquote>
 *
 * @author Hamza Assada 03/06/2025 (7amza.it@gmail.com)
 */
public interface AssociationResolver {
    <T> T resolveEntity(Class<T> entityClass, String keyField, Object keyValue);
}
