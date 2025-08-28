package org.nmcpye.datarun.importer.identifier;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author Hamza Assada 03/06/2025 (7amza.it@gmail.com)
 */
@Component("uid")
public class UidIdentifierStrategy implements IdentifierStrategy {
    public String extractIdentifier(Map<String, Object> data) {
        return data.get("uid").toString();
    }
}
