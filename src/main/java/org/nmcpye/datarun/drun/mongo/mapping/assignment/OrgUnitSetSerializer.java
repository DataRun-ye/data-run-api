package org.nmcpye.datarun.drun.mongo.mapping.assignment;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.nmcpye.datarun.domain.OrgUnit;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

public class OrgUnitSetSerializer extends JsonSerializer<Set<OrgUnit>> {
    @Override
    public void serialize(Set<OrgUnit> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        Set<String> uids = value.stream().map(OrgUnit::getUid).collect(Collectors.toSet());
        gen.writeObject(uids);
    }
}
