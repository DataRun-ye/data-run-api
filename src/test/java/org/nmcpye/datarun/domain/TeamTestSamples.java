//package org.nmcpye.datarun.domain;
//
//import org.nmcpye.datarun.team.Team;
//
//import java.util.Random;
//import java.util.UUID;
//import java.util.concurrent.atomic.AtomicInteger;
//import java.util.concurrent.atomic.AtomicLong;
//
//public class TeamTestSamples {
//
//    private static final Random random = new Random();
//    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));
//    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));
//
//    public static Team getTeamSample1() {
//        return new Team()
//            .id(1L)
//            .uid("uid1")
//            .code("code1")
//            .name("name1")
//            .description("description1")
//            .createdBy("createdBy1")
//            .lastModifiedBy("lastModifiedBy1");
//    }
//
//    public static Team getTeamSample2() {
//        return new Team()
//            .id(2L)
//            .uid("uid2")
//            .code("code2")
//            .name("name2")
//            .description("description2")
//            .createdBy("createdBy2")
//            .lastModifiedBy("lastModifiedBy2");
//    }
//
//    public static Team getTeamRandomSampleGenerator() {
//        return new Team()
//            .id(longCount.incrementAndGet())
//            .uid(UUID.randomUUID().toString())
//            .code(UUID.randomUUID().toString())
//            .name(UUID.randomUUID().toString())
//            .description(UUID.randomUUID().toString())
//            .createdBy(UUID.randomUUID().toString())
//            .lastModifiedBy(UUID.randomUUID().toString());
//    }
//}
