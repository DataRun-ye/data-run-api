//package org.nmcpye.datarun.drun.mongo.mapping.serialization;
//
//import com.fasterxml.jackson.core.JsonParser;
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.DeserializationContext;
//import com.fasterxml.jackson.databind.JsonDeserializer;
//import org.nmcpye.datarun.domain.OrgUnit;
//import org.nmcpye.datarun.drun.postgres.service.OrgUnitServiceCustom;
//
//import java.io.IOException;
//import java.util.Objects;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//public class OrgUnitUidsSetDeserializer extends JsonDeserializer<Set<OrgUnit>> {
//
//
//    final private OrgUnitServiceCustom orgUnitService; // Service to fetch OrgUnit by UID
//
//    public OrgUnitUidsSetDeserializer(OrgUnitServiceCustom orgUnitService) {
//        this.orgUnitService = orgUnitService;
//    }
//
//    @Override
//    public Set<OrgUnit> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
//        Set<String> uids = p.readValuesAs(new TypeReference<Set<String>>() {
//        }).next();
//        return uids.stream()
//            .map(uid -> orgUnitService.findAssignedByUid(uid).orElse(null)) // Handle Optional
//            .filter(Objects::nonNull) // Filter out null values
////            .map(OrgUnitReference::new)
//            .collect(Collectors.toSet());
//    }
//}
