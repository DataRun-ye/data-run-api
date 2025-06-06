package org.nmcpye.datarun.importer.identifier;

import java.util.Map;

/**
 * Generic strategy interface to resolve domain entities by different key types.
 *
 * @author Hamza Assada 03/06/2025 <7amza.it@gmail.com>
 */
public interface IdentifierStrategy {
    Object extractIdentifier(Map<String, Object> data);
}
