package org.nmcpye.datarun.web.v2.idempotency;

import org.nmcpye.datarun.web.v2.config.V2Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Deletes expired idempotency keys, in batches of {@code datarun.v2.idempotency.purge-batch-size}, every
 * {@code datarun.v2.idempotency.purge-interval}. Expiry does not wait for it: a claim treats an expired key as
 * absent. The rows are few (one per command, kept 72 hours), so a purge is short.
 */
@Component
public class IdempotencyPurge {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyPurge.class);

    private final IdempotencyStore store;
    private final TransactionTemplate transactions;
    private final V2Properties properties;

    public IdempotencyPurge(IdempotencyStore store, PlatformTransactionManager transactionManager,
                            V2Properties properties) {
        this.store = store;
        this.transactions = new TransactionTemplate(transactionManager);
        this.properties = properties;
    }

    @Scheduled(initialDelayString = "PT10M", fixedDelayString = "${datarun.v2.idempotency.purge-interval:PT1H}")
    public void purgeOnSchedule() {
        int deleted = purge();
        if (deleted > 0) {
            log.info("Purged {} expired v2 idempotency keys", deleted);
        }
    }

    /** Deletes every key expired now, one batch per transaction, and says how many. */
    public int purge() {
        int batch = properties.getIdempotency().getPurgeBatchSize();
        int total = 0;
        int deleted;
        do {
            Integer count = transactions.execute(status -> store.purgeExpired(batch));
            deleted = count == null ? 0 : count;
            total += deleted;
        } while (deleted == batch);
        return total;
    }
}
