//package org.nmcpye.datarun.domain;
//
//import java.util.Random;
//import java.util.UUID;
//import java.util.concurrent.atomic.AtomicLong;
//
//public class ActivityTestSamples {
//
//    private static final Random random = new Random();
//    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));
//
//    public static Activity getActivitySample1() {
//        return new Activity().id(1L).uid("uid1").code("code1").name("name1").createdBy("createdBy1").lastModifiedBy("lastModifiedBy1");
//    }
//
//    public static Activity getActivitySample2() {
//        return new Activity().id(2L).uid("uid2").code("code2").name("name2").createdBy("createdBy2").lastModifiedBy("lastModifiedBy2");
//    }
//
//    public static Activity getActivityRandomSampleGenerator() {
//        return new Activity()
//            .id(longCount.incrementAndGet())
//            .uid(UUID.randomUUID().toString())
//            .code(UUID.randomUUID().toString())
//            .name(UUID.randomUUID().toString())
//            .createdBy(UUID.randomUUID().toString())
//            .lastModifiedBy(UUID.randomUUID().toString());
//    }
//}
