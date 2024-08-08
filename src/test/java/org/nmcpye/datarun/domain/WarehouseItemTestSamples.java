package org.nmcpye.datarun.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class WarehouseItemTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static WarehouseItem getWarehouseItemSample1() {
        return new WarehouseItem().id(1L).uid("uid1").code("code1").name("name1").description("description1");
    }

    public static WarehouseItem getWarehouseItemSample2() {
        return new WarehouseItem().id(2L).uid("uid2").code("code2").name("name2").description("description2");
    }

    public static WarehouseItem getWarehouseItemRandomSampleGenerator() {
        return new WarehouseItem()
            .id(longCount.incrementAndGet())
            .uid(UUID.randomUUID().toString())
            .code(UUID.randomUUID().toString())
            .name(UUID.randomUUID().toString())
            .description(UUID.randomUUID().toString());
    }
}
