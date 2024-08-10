package org.nmcpye.datarun.drun.mongo.mapping.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.nmcpye.datarun.domain.Activity;
import org.nmcpye.datarun.drun.postgres.service.ActivityServiceCustom;

import java.io.IOException;

public class ActivityDeserializer extends JsonDeserializer<Activity> {

    final private ActivityServiceCustom activityServiceCustom;  // or a repository to fetch OrgUnit by UID

    public ActivityDeserializer(ActivityServiceCustom activityServiceCustom) {
        this.activityServiceCustom = activityServiceCustom;
    }

    @Override
    public Activity deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String uid = p.getValueAsString();
        return activityServiceCustom.findByUid(uid).orElseThrow();  // Fetch OrgUnit object using the UID
    }
}
