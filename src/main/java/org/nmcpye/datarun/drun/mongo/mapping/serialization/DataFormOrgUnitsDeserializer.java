//package org.nmcpye.datarun.drun.mongo.mapping.serialization;
//
//import com.fasterxml.jackson.core.JsonParser;
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
///**
// * to deserialize DataForms from JSON
// **/
//
//public class DataFormOrgUnitsDeserializer
//    extends JsonDeserializer<Set<OrgUnit>> {
//    final private OrgUnitServiceCustom orgUnitService; // Service to fetch OrgUnit by UID
//
//    public DataFormOrgUnitsDeserializer(OrgUnitServiceCustom orgUnitService) {
//        this.orgUnitService = orgUnitService;
//    }
//
//    @Override
//    public Set<OrgUnit> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
//        var uidList = p.readValueAs(String[].class);
//        return Set.of(uidList).stream()
//            .map(uid -> orgUnitService.findAssignedByUid(uid).orElse(null)) // Handle Optional
//            .filter(Objects::nonNull) // Filter out null values
////            .map(OrgUnitReference::new)
//            .collect(Collectors.toSet());
//    }
//}
