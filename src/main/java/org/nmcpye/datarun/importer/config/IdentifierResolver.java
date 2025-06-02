package org.nmcpye.datarun.importer.config;

import java.util.Optional;

/**
 * Generic interface to resolve domain entities by different key types.
 *
 * @author Hamza Assada (02-06-2025), <7amza.it@gmail.com>
 */
public interface IdentifierResolver<T> {
    Optional<T> resolve(KeyType keyType, String keyValue);
}
