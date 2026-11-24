package org.nmcpye.datarun.etl.orchestrator;

/**
 * What one ETL run (one claimed batch) did. {@code stopped}: a lost connection or an interrupt ended the batch
 * early and its unfinished rows were released.
 */
public record EtlRunResult(int claimed, int succeeded, int failed, boolean stopped) {
}
