-- The transaction every check runs in, with the limits on its statements. check.sh starts its run with this file, and
-- the tests that run a check start theirs with it, so both run under the same limits.
--
-- One read-only, repeatable-read transaction, so every count comes from the same snapshot; the runner ends it with
-- ROLLBACK. The database host may have little memory, so every statement runs with work_mem 8MB, no parallel
-- workers, a 5-minute timeout and at most 1GB of temporary files. temp_file_limit may be set only by a superuser, so
-- it comes before the switch to the read-only role pg_read_all_data.
BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY;
SET LOCAL work_mem = '8MB';
SET LOCAL max_parallel_workers_per_gather = 0;
SET LOCAL statement_timeout = '5min';
SET LOCAL lock_timeout = '5s';
SET LOCAL idle_in_transaction_session_timeout = '2min';
SET LOCAL jit = off;
SET LOCAL client_connection_check_interval = '10s';
SET LOCAL temp_file_limit = '1GB';
SET LOCAL ROLE pg_read_all_data;
