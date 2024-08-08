package org.nmcpye.datarun.drun.mongo.mapping.assignment;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.nmcpye.datarun.domain.OrgUnit;
import org.nmcpye.datarun.drun.postgres.service.OrgUnitRelationalServiceCustom;

import java.io.IOException;

public class OrgUnitDeserializer extends JsonDeserializer<OrgUnit> {

    final private OrgUnitRelationalServiceCustom orgUnitService;  // or a repository to fetch OrgUnit by UID

    public OrgUnitDeserializer(OrgUnitRelationalServiceCustom orgUnitService) {
        this.orgUnitService = orgUnitService;
    }

    @Override
    public OrgUnit deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String uid = p.getValueAsString();
        return orgUnitService.findByUid(uid).orElse(null);  // Fetch OrgUnit object using the UID
    }
}
