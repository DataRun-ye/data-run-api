//package org.nmcpye.datarun.utils;
//
//import org.nmcpye.datarun.mongo.domain.DataForm;
//import org.nmcpye.datarun.mongo.domain.datafield.AbstractField;
//import org.springframework.data.mongodb.core.schema.JsonSchemaObject;
//import org.springframework.data.mongodb.core.schema.JsonSchemaProperty;
//import org.springframework.data.mongodb.core.schema.MongoJsonSchema;
//import org.springframework.data.mongodb.core.schema.TypedJsonSchemaObject;
//import org.springframework.stereotype.Service;
//
//import java.util.ArrayList;
//import java.util.List;
//
//@Service
//public class FormTemplateSchema {
////    @Autowired
////    private FormSchemaRepository formSchemaRepository;
////
////    public MongoJsonSchema generateAndStoreSchema(DataForm formTemplate) {
////        MongoJsonSchema schema = generateSchema(formTemplate);
////        FormSchema formSchema = new FormSchema();
////        formSchema.setFormUid(formTemplate.getUid());
////        formSchema.setVersion(formTemplate.getVersion());
////        formSchema.setSchema(schema);
////        formSchemaRepository.save(formSchema);
////        return schema;
////    }
//
//    public MongoJsonSchema generateSchema(DataForm formTemplate) {
//        MongoJsonSchema.MongoJsonSchemaBuilder schemaBuilder = MongoJsonSchema.builder();
//
//        // Add required top-level fields
//        schemaBuilder.required("uid", "version", "form");
//
//        // Add properties for top-level fields
//        schemaBuilder.properties(parseFields(formTemplate.getFields()));
//
//        return schemaBuilder.build();
//    }
//
//    private JsonSchemaProperty[] parseFields(List<AbstractField> fields) {
//        TypedJsonSchemaObject.ObjectJsonSchemaObject fieldSchema = new TypedJsonSchemaObject.ObjectJsonSchemaObject();
//        List<JsonSchemaProperty> properties = new ArrayList<>();
//        for (var field : fields) {
//
//            var mappedField = mapFieldType(field);
////            if (((DefaultField) field).isMandatory()) {
////                mappedField.isRequired();
////            }
//            properties.add(mappedField);
//        }
//
//        return properties.toArray(new JsonSchemaProperty[0]);
//    }
//
//    private JsonSchemaProperty mapFieldType(AbstractField field) {
//        return switch (field.getType()) {
//            case Integer, IntegerNegative, IntegerPositive, IntegerZeroOrPositive, UnitInterval ->
//                JsonSchemaProperty.int64(field.getName());
//            case Number, Percentage -> JsonSchemaProperty.number(field.getName());
//            case Date, DateTime, Time -> JsonSchemaProperty.timestamp(field.getName());
//            case Boolean, TrueOnly -> JsonSchemaProperty.bool(field.getName());
////            case SelectOne, SelectMulti -> JsonSchemaProperty.string(field.getName()).possibleValues(((org
////                .nmcpye.datarun.mongo.domain.datafield.SelectOne) field).getOptions());
//
//            case Section -> JsonSchemaProperty.object(field.getName())
//                .properties(
//                    parseFields(((org.nmcpye.datarun.mongo.domain.datafield.Section) field).getFields()));
//            case RepeatableSection -> JsonSchemaProperty.array(field.getName()).items(
//                parseFields(((org.nmcpye.datarun.mongo.domain.datafield.Section) field).getFields()));
//
//            case Age -> JsonSchemaProperty.int32(field.getName());
//            default -> JsonSchemaProperty.string(field.getName());
//        };
//    }
//
//    private JsonSchemaObject.Type mapType(AbstractField field) {
//        return switch (field.getType()) {
//            case Integer, IntegerNegative, IntegerPositive, IntegerZeroOrPositive, UnitInterval ->
//                JsonSchemaObject.Type.INT_64;
//            case Number, Percentage -> JsonSchemaObject.Type.DOUBLE;
//            case Date, DateTime, Time -> JsonSchemaProperty.Type.DATE;
//            case Boolean, TrueOnly -> JsonSchemaObject.Type.BOOLEAN;
//
//            case Section -> JsonSchemaObject.Type.OBJECT;
//            case RepeatableSection -> JsonSchemaObject.Type.ARRAY;
//
//            case Age -> JsonSchemaObject.Type.INT_32;
//            default -> JsonSchemaObject.Type.STRING;
//        };
//    }
//}
//
////public boolean validateDynamicProperty(Map<String, Object> dynamicProperty, String schemaString) {
////    JSONObject jsonSchema = new JSONObject(schemaString);
////    Schema schema = SchemaLoader.load(jsonSchema);
////
////    JSONObject jsonObject = new JSONObject(dynamicProperty);
////    schema.validate(jsonObject); // Throws ValidationException if invalid
////    return true;
////}
