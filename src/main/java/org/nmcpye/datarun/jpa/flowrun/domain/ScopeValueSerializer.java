package org.nmcpye.datarun.jpa.flowrun.domain;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.nmcpye.datarun.jpa.flowtype.ScopePropertyType;

import java.io.IOException;
import java.util.Map;

/**
 * @author Hamza Assada 08/06/2025 <7amza.it@gmail.com>
 */
public class ScopeValueSerializer extends JsonSerializer<Map<String, Object>> {
    // Outputs: {"TEAM": {"value": "teamA", "type": "TEAM"}}
    @Override
    public void serialize(Map<String, Object> scopes, JsonGenerator gen, SerializerProvider provider) throws IOException {
        // Add type metadata during serialization
        gen.writeStartObject();
        for (Map.Entry<String, Object> entry : scopes.entrySet()) {
            gen.writeFieldName(entry.getKey());
            gen.writeStartObject();
            gen.writeObjectField("value", entry.getValue());
            final var scopeType = ScopePropertyType.valueOf(entry.getKey());
            gen.writeObjectField("type", scopeType.name());
            gen.writeEndObject();
        }
        gen.writeEndObject();
    }
}
