package org.nmcpye.datarun.etl.orchestrator;

/** What one ETL run (one claimed batch) did. */
public record EtlRunResult(int claimed, int succeeded, int failed) {
}
