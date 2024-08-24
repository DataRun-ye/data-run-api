package org.nmcpye.datarun.drun.mongo.mapping.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.nmcpye.datarun.drun.mongo.domain.DataOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class DataOptionSerializer extends JsonSerializer<DataOption> {
    private static final Logger log = LoggerFactory.getLogger(DataOptionSerializer.class);

    @Override
    public void serialize(DataOption dataOption, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        log.info("\n Serializing: {}", dataOption.toString());
        gen.writeStartObject();
        gen.writeStringField("listName", dataOption.getListName());
        gen.writeStringField("name", dataOption.getName());
        if (dataOption.getOrder() != null) {
            gen.writeNumberField("order", dataOption.getOrder());
        }
        gen.writeObjectField("label", dataOption.getLabel());

        // Flatten properties map
        for (Map.Entry<String, Object> entry : dataOption.getProperties().entrySet()) {
            gen.writeObjectField(entry.getKey(), entry.getValue());
        }

        gen.writeEndObject();
    }
}
