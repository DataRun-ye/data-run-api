//package org.nmcpye.datarun.drun.mongo.service.converter;
//
//import com.mongodb.DBObject;
//import org.nmcpye.datarun.drun.mongo.domain.DataForm;
//import org.springframework.core.convert.converter.Converter;
//import org.springframework.data.convert.ReadingConverter;
//
//import java.util.Map;
//
//@ReadingConverter
//public class FormOptionReadingConverter implements Converter<DBObject, DataForm> {
//
//    @Override
//    public DataForm convert(DBObject source) {
//        DataForm field = new DataForm();
//        field.setUid((String) source.get("id"));
//        field.setName((String) source.get("name"));
////        field.setType((String) source.get("type"));
//        field.setLabel((Map<String, String>) source.get("label"));
////        field.setMandatory((Boolean) source.get("mandatory"));
//
//        // Handle custom ID
//        field.setUid((String) source.get("id"));
//
//        // Set other fields as necessary
//        return field;
//    }
//}
