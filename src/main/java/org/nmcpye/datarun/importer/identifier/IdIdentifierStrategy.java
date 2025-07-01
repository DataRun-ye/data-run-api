package org.nmcpye.datarun.importer.identifier;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author Hamza Assada 03/06/2025 (7amza.it@gmail.com)
 */
@Component("id")
public class IdIdentifierStrategy implements IdentifierStrategy {
    public Long extractIdentifier(Map<String, Object> data) {
        return Long.parseLong(data.get("id").toString());
    }
}
