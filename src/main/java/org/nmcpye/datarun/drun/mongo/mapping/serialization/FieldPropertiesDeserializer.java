//package org.nmcpye.datarun.drun.mongo.mapping.serialization;
//
//import com.fasterxml.jackson.core.JsonParser;
//import com.fasterxml.jackson.databind.DeserializationContext;
//import com.fasterxml.jackson.databind.JsonMappingException;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
//import org.nmcpye.datarun.drun.mongo.domain.datafield.FieldProperties;
//import org.nmcpye.datarun.drun.mongo.domain.datafield.FieldPropertiesFactory;
//import org.nmcpye.datarun.drun.mongo.domain.enumeration.ValueType;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.IOException;
//
//public class FieldPropertiesDeserializer extends StdDeserializer<FieldProperties> {
//    private static final Logger log = LoggerFactory.getLogger(FieldPropertiesDeserializer.class);
//
//    public FieldPropertiesDeserializer() {
//        super(FieldProperties.class);
//    }
//
//    //    @Override
////    public FieldProperties deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
////        ObjectNode node = jp.getCodec().readTree(jp);
////        String type = node.get("type").asText();
////        log.info("\n Deserializing properties of type: {}", type);
////
////        if (type.equals("ScannedCode")) {
////            return jp.getCodec().treeToValue(node, ScannedCodeProperties.class);
////        }
////        return jp.getCodec().treeToValue(node, DefaultFieldProperties.class);
////    }
//    @Override
//    public FieldProperties deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
//        JsonNode node = jp.getCodec().readTree(jp);
//        String type = node.get("type").asText();
//        Class<? extends FieldProperties> clazz = FieldPropertiesFactory.getPropertyClass(ValueType.valueOf(type));
//        log.info("\n Deserializing properties of type: {}, class {} ", type, clazz.getSimpleName());
//
//        // Deserialize into the appropriate class
//        try {
//            return jp.getCodec().treeToValue(node, clazz);
//        } catch (IllegalArgumentException e) {
//            throw new JsonMappingException(jp, "Invalid properties for type: " + type, e);
//        }
//    }
//}
