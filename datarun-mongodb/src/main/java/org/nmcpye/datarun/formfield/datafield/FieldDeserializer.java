package org.nmcpye.datarun.formfield.datafield;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.nmcpye.datarun.mongo.domain.enumeration.ValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class FieldDeserializer extends StdDeserializer<AbstractField> {
    private static final Logger log = LoggerFactory.getLogger(FieldDeserializer.class);

    public FieldDeserializer() {
        super(AbstractField.class);
    }

    @Override
    public AbstractField deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        String type = node.get("type").asText();
        Class<? extends AbstractField> clazz = FormFieldFactory.getPropertyClass(ValueType.valueOf(type));
//        log.info("\n Deserializing properties of type: {}, class {} ", type, clazz.getSimpleName());

        // Deserialize into the appropriate class
        try {
            return jp.getCodec().treeToValue(node, clazz);
        } catch (IllegalArgumentException e) {
            throw new JsonMappingException(jp, "Invalid properties for type: " + type, e);
        }
    }
}
