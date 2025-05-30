//package org.nmcpye.datarun.drun.mongo.mapping.serialization;
//
//import com.fasterxml.jackson.core.JsonGenerator;
//import com.fasterxml.jackson.databind.JsonSerializer;
//import com.fasterxml.jackson.databind.SerializerProvider;
//import org.nmcpye.datarun.orgunit.OrgUnit;
//
//import java.io.IOException;
//import java.util.Collection;
//import java.util.Set;
//import java.util.stream.Collectors;
//
///**
// * to serialize DataForm of arbitrary types into JSON
// **/
//
//public class DataFormSerializer extends JsonSerializer<Collection<OrgUnit>> {
//    @Override
//    public void serialize(Collection<OrgUnit> value,
//                          JsonGenerator gen,
//                          SerializerProvider serializers) throws IOException {
//
//        Set<String> uids = value.stream().map(OrgUnit::getUid).collect(Collectors.toSet());
//        gen.writeObject(uids);
//    }
//}
