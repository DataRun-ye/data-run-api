package org.nmcpye.datarun.drun.mongo.mapping.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.nmcpye.datarun.drun.mongo.domain.DataOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class DataOptionDeserializer extends StdDeserializer<DataOption> {
    private static final Logger log = LoggerFactory.getLogger(DataOptionSerializer.class);

    public DataOptionDeserializer() {
        super(DataOption.class);
    }

    @Override
    public DataOption deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        log.info("\n Deserializing: dataOption");
        JsonNode node = jp.getCodec().readTree(jp);
        DataOption dataOption = new DataOption();

        // Set known fields
        dataOption.setListName(node.get("listName").asText());
        dataOption.setName(node.get("name").asText());
        if (node.has("order")) {
            dataOption.setOrder(node.get("order").asInt());
        }
        if (node.has("label")) {
            Map<String, String> labelMap = new HashMap<>();
            JsonNode labelNode = node.get("label");
            labelNode.fields().forEachRemaining(field -> labelMap.put(field.getKey(), field.getValue().asText()));
            dataOption.setLabel(labelMap);
        }

        // Handle additional properties
        Map<String, Object> properties = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String fieldName = entry.getKey();
            if (!"listName".equals(fieldName) && !"name".equals(fieldName) && !"order".equals(fieldName) && !"label".equals(fieldName)) {
                JsonNode valueNode = entry.getValue();
                if (valueNode.isTextual()) {
                    properties.put(fieldName, valueNode.asText());
                } else if (valueNode.isNumber()) {
                    if (valueNode.isIntegralNumber()) {
                        properties.put(fieldName, valueNode.asInt());
                    } else {
                        properties.put(fieldName, valueNode.asDouble());
                    }
                } else if (valueNode.isBoolean()) {
                    properties.put(fieldName, valueNode.asBoolean());
                } else if (valueNode.isObject()) {
                    properties.put(fieldName, valueNode.toString()); // or process further if needed
                } else {
                    properties.put(fieldName, valueNode.toString());
                }
            }
        }
        dataOption.setProperties(properties);

        return dataOption;
    }
}

