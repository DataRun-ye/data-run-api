/**
 * Goals & guarantees we want when normalizing:
 * <p>
 * * **Atomic publication intent**: when a submission is saved, there must be a durable record of "process this submission" written in the same DB transaction (outbox row). If the app crashes after commit, the relay can pick it up.
 * * **Exactly-once or at-least-once processing with idempotence**: ETL work can run multiple times safely (use upserts / idempotent inserts).
 * * **Safe claiming / concurrency**: relay(s) can run in parallel and must not process the same outbox row twice concurrently.
 * * **Retries & DLQ** for transient vs permanent failures.
 * * **Observability & retention**: processed/unprocessed rows tracked, old rows purged/archived.
 *
 * @author Hamza Assada 15/08/2025 (7amza.it@gmail.com)
 */
package org.nmcpye.datarun.jpa.etl;

