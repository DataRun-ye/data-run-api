package org.nmcpye.datarun.mongo.mapping.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.nmcpye.datarun.domain.Activity;
import org.nmcpye.datarun.drun.postgres.service.ActivityService;

import java.io.IOException;

public class ActivityDeserializer extends JsonDeserializer<Activity> {

    final private ActivityService activityService;  // or a repository to fetch OrgUnit by UID

    public ActivityDeserializer(ActivityService activityService) {
        this.activityService = activityService;
    }

    @Override
    public Activity deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String uid = p.getValueAsString();
        return activityService.findByUid(uid).orElseThrow();  // Fetch OrgUnit object using the UID
    }
}
