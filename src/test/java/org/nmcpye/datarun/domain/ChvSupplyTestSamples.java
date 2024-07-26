package org.nmcpye.datarun.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ChvSupplyTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static ChvSupply getChvSupplySample1() {
        return new ChvSupply()
            .id(1L)
            .uid("uid1")
            .code("code1")
            .name("name1")
            .previousBalance(1)
            .newSupply(1)
            .consumed(1)
            .lostCorrupt(1)
            .remaining(1)
            .comment("comment1")
            .createdBy("createdBy1")
            .lastModifiedBy("lastModifiedBy1");
    }

    public static ChvSupply getChvSupplySample2() {
        return new ChvSupply()
            .id(2L)
            .uid("uid2")
            .code("code2")
            .name("name2")
            .previousBalance(2)
            .newSupply(2)
            .consumed(2)
            .lostCorrupt(2)
            .remaining(2)
            .comment("comment2")
            .createdBy("createdBy2")
            .lastModifiedBy("lastModifiedBy2");
    }

    public static ChvSupply getChvSupplyRandomSampleGenerator() {
        return new ChvSupply()
            .id(longCount.incrementAndGet())
            .uid(UUID.randomUUID().toString())
            .code(UUID.randomUUID().toString())
            .name(UUID.randomUUID().toString())
            .previousBalance(intCount.incrementAndGet())
            .newSupply(intCount.incrementAndGet())
            .consumed(intCount.incrementAndGet())
            .lostCorrupt(intCount.incrementAndGet())
            .remaining(intCount.incrementAndGet())
            .comment(UUID.randomUUID().toString())
            .createdBy(UUID.randomUUID().toString())
            .lastModifiedBy(UUID.randomUUID().toString());
    }
}
