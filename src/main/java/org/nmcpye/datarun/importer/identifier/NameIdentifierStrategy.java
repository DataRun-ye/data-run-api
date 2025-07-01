package org.nmcpye.datarun.importer.identifier;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author Hamza Assada 03/06/2025 (7amza.it@gmail.com)
 */
@Component("name")
public class NameIdentifierStrategy implements IdentifierStrategy {
    public String extractIdentifier(Map<String, Object> data) {
        return data.get("name").toString();
    }
}
