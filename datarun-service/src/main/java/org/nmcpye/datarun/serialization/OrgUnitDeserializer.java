package org.nmcpye.datarun.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.nmcpye.datarun.drun.postgres.domain.OrgUnit;
import org.nmcpye.datarun.drun.postgres.service.OrgUnitService;

import java.io.IOException;

public class OrgUnitDeserializer extends JsonDeserializer<OrgUnit> {

    final private OrgUnitService orgUnitService;  // or a repository to fetch OrgUnit by UID

    public OrgUnitDeserializer(OrgUnitService orgUnitService) {
        this.orgUnitService = orgUnitService;
    }

    @Override
    public OrgUnit deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String uid = p.getValueAsString();
        return orgUnitService.findByUid(uid).orElse(null);  // Fetch OrgUnit object using the UID
    }
}
