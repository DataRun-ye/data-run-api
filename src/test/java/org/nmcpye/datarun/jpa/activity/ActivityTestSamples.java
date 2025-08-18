package org.nmcpye.datarun.jpa.activity;

import org.nmcpye.datarun.common.uidgenerate.CodeGenerator;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class ActivityTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));
    private static final String ulidPrefix = "01JYF39TAE1TZAKVVSZDNA0RF";

    //    private static final String ulid = ;
    public static Activity getActivitySample1() {
        final var activity = new Activity();
        activity.setId(ulidPrefix + "1");
        activity.setUid("uid1");
        activity.setCode("code1");
        activity.setName("name1");
        activity.setCreatedBy("createdBy1");
        activity.setLastModifiedBy("lastModifiedBy1");
        return activity;
    }

    public static Activity getActivitySample2() {
        final var activity = new Activity();
        activity.setId(ulidPrefix + "2");
        activity.setUid("01JYF39TAE1TZAKVVSZDNA0RFJ");
        activity.setCode("code1");
        activity.setName("name1");
        activity.setCreatedBy("createdBy1");
        activity.setLastModifiedBy("lastModifiedBy1");
        return activity;
    }

    public static Activity getActivityRandomSampleGenerator() {
        final var activity = new Activity();
        activity.setId(CodeGenerator.nextUlid());
        activity.setUid(CodeGenerator.generateUid());
        activity.setCode(CodeGenerator.nextUlid());
        activity.setName(CodeGenerator.nextUlid());
        activity.setCreatedBy(CodeGenerator.nextUlid());
        activity.setLastModifiedBy(CodeGenerator.nextUlid());
        return activity;
    }
}
