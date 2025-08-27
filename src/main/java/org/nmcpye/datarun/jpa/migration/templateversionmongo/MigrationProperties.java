package org.nmcpye.datarun.jpa.migration.templateversionmongo;

import lombok.Data;

/**
 * @author Hamza Assada - 7amza.it@gmail.com
 * @since 24/08/2025
 */
//@Configuration
//@ConfigurationProperties(prefix = "migration.template-version")
@Data
public class MigrationProperties {
    /** Number of Mongo docs to fetch per page/batch. */
    private int batchSize = 500;

    /** Max attempts to retry saving a batch on transient failures. */
    private int maxRetries = 3;

    /** Backoff between retry attempts in milliseconds. */
    private long retryBackoffMs = 2000L;

    /** If true, no writes are performed. Useful for dry-run. */
    private boolean dryRun = false;

    /** If true, skip documents whose id already exists in Postgres. */
    private boolean skipExisting = true;

    /** Start from this mongo id (exclusive) - optional resume aid. */
    private String resumeAfterUid;

    /** Optional limit on total records to migrate (useful for testing). 0 = no limit */
    private long limit = 0L;
}
