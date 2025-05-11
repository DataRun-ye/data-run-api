//package org.nmcpye.datarun.drun.mongo.mapping.assignment;
//
//import com.fasterxml.jackson.core.JsonGenerator;
//import com.fasterxml.jackson.databind.JsonSerializer;
//import com.fasterxml.jackson.databind.SerializerProvider;
//import org.nmcpye.datarun.drun.postgres.domain.OrgUnit;
//
//import java.io.IOException;
//
//public class OrgUnitSerializer extends JsonSerializer<OrgUnit> {
//    @Override
//    public void serialize(OrgUnit value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
//        gen.writeString(value.getUid());
//    }
//}
