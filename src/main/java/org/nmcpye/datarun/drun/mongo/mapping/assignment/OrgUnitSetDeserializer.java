package org.nmcpye.datarun.drun.mongo.mapping.assignment;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.nmcpye.datarun.domain.OrgUnit;
import org.nmcpye.datarun.drun.postgres.service.OrgUnitRelationalServiceCustom;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class OrgUnitSetDeserializer extends JsonDeserializer<Set<OrgUnit>> {


    final private OrgUnitRelationalServiceCustom orgUnitService; // Service to fetch OrgUnit by UID

    public OrgUnitSetDeserializer(OrgUnitRelationalServiceCustom orgUnitService) {
        this.orgUnitService = orgUnitService;
    }

    @Override
    public Set<OrgUnit> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        Set<String> uids = p.readValuesAs(new TypeReference<Set<String>>() {
        }).next();
        return uids.stream()
            .map(uid -> orgUnitService.findByUid(uid).orElse(null)) // Handle Optional
            .filter(Objects::nonNull) // Filter out null values
            .collect(Collectors.toSet());
    }
}
