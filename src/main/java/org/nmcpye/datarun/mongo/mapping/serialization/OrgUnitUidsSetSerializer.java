package org.nmcpye.datarun.mongo.mapping.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.nmcpye.datarun.drun.postgres.service.OrgUnitServiceCustom;

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * to serialize DataForm of arbitrary types into JSON
 **/
public class OrgUnitUidsSetSerializer extends JsonSerializer<Collection<String>> {
    final private OrgUnitServiceCustom orgUnitService; // Service to fetch OrgUnit by UID

    public OrgUnitUidsSetSerializer(OrgUnitServiceCustom orgUnitService) {
        this.orgUnitService = orgUnitService;
    }

    @Override
    public void serialize(Collection<String> value,
                          JsonGenerator gen,
                          SerializerProvider serializers) throws IOException {
        Set<OrgUnitReference> orgUnits = value.stream()
            .map(uid -> orgUnitService.findByUid(uid).orElse(null)) // Handle Optional
            .filter(Objects::nonNull)
            .map(OrgUnitReference::new)
            .collect(Collectors.toSet());
        gen.writeObject(orgUnits);
    }
}
