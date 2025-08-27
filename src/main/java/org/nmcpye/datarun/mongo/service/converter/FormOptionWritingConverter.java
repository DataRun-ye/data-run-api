//package org.nmcpye.datarun.drun.mongo.service.converter;
//
//import com.mongodb.BasicDBObject;
//import com.mongodb.DBObject;
//import org.nmcpye.datarun.drun.mongo.domain.DataForm;
//import org.springframework.core.convert.converter.Converter;
//import org.springframework.data.convert.WritingConverter;
//
//import java.util.Map;
//
//@WritingConverter
//public class FormOptionWritingConverter implements Converter<DataForm, DBObject> {
//
//    @Override
//    public DBObject convert(DataForm source) {
//        BasicDBObject dbObject = new BasicDBObject();
//        dbObject.put("name", source.getName());
////        dbObject.put("type", source.getType());
//        dbObject.put("label", getLabel(source));
////        dbObject.put("mandatory", source.isMandatory());
//
//        // Custom ID generation logic
//        String customId = generateCustomId(source);
//        dbObject.put("id", customId);
//
//        // Add other fields as necessary
//        return dbObject;
//    }
//
//    public Map<String, String> getLabel(DataForm form) {
//        if (form.getLabel() == null) {
//            return Map.of("en", form.getName());
//        }
//        return form.getLabel();
//    }
//
//    private String generateCustomId(DataForm form) {
//        // Example: Generate ID based on form UID and field name
//        return form.getUid() + "_" + form.getName().toUpperCase();
//    }
//}
//
