//package org.nmcpye.datarun.domain;
//
//import java.util.Random;
//import java.util.UUID;
//import java.util.concurrent.atomic.AtomicLong;
//
//public class ProjectTestSamples {
//
//    private static final Random random = new Random();
//    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));
//
//    public static Project getProjectSample1() {
//        return new Project().id(1L).id("uid1").code("code1").name("name1").createdBy("createdBy1").lastModifiedBy("lastModifiedBy1");
//    }
//
//    public static Project getProjectSample2() {
//        return new Project().id(2L).id("uid2").code("code2").name("name2").createdBy("createdBy2").lastModifiedBy("lastModifiedBy2");
//    }
//
//    public static Project getProjectRandomSampleGenerator() {
//        return new Project()
//            .id(longCount.incrementAndGet())
//            .id(UUID.randomUUID().toString())
//            .code(UUID.randomUUID().toString())
//            .name(UUID.randomUUID().toString())
//            .createdBy(UUID.randomUUID().toString())
//            .lastModifiedBy(UUID.randomUUID().toString());
//    }
//}
