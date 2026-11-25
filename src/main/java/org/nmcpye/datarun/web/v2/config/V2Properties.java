package org.nmcpye.datarun.web.v2.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.time.Duration;

/** Settings of the v2 conventions, under {@code datarun.v2}. The defaults are the published contract. */
@ConfigurationProperties(prefix = "datarun.v2")
public class V2Properties {

    private final Commands commands = new Commands();
    private final Idempotency idempotency = new Idempotency();

    public Commands getCommands() {
        return commands;
    }

    public Idempotency getIdempotency() {
        return idempotency;
    }

    public static class Commands {

        /**
         * The longest a command waits for any lock in its transaction: a duplicate waiting on its idempotency key
         * (then 409 {@code request-in-progress}), or the resource's row held by another writer (then 409
         * {@code resource-busy}). It keeps a burst of retries from holding the connection pool that phone uploads
         * need.
         */
        private Duration lockTimeout = Duration.ofSeconds(5);

        /** The largest command body read; a larger one is 413 {@code payload-too-large}. */
        private DataSize maxBodySize = DataSize.ofMegabytes(1);

        public Duration getLockTimeout() {
            return lockTimeout;
        }

        public void setLockTimeout(Duration lockTimeout) {
            this.lockTimeout = lockTimeout;
        }

        public DataSize getMaxBodySize() {
            return maxBodySize;
        }

        public void setMaxBodySize(DataSize maxBodySize) {
            this.maxBodySize = maxBodySize;
        }
    }

    public static class Idempotency {

        /** How long a key is remembered after its command; afterwards the same key runs as a new request. */
        private Duration retention = Duration.ofHours(72);

        /** The pause between two purges of expired keys. */
        private Duration purgeInterval = Duration.ofHours(1);

        /** Expired keys deleted per statement of a purge. */
        private int purgeBatchSize = 1000;

        public Duration getRetention() {
            return retention;
        }

        public void setRetention(Duration retention) {
            this.retention = retention;
        }

        public Duration getPurgeInterval() {
            return purgeInterval;
        }

        public void setPurgeInterval(Duration purgeInterval) {
            this.purgeInterval = purgeInterval;
        }

        public int getPurgeBatchSize() {
            return purgeBatchSize;
        }

        public void setPurgeBatchSize(int purgeBatchSize) {
            this.purgeBatchSize = purgeBatchSize;
        }
    }
}
