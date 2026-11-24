#!/usr/bin/env bash
# Read-only reconciliation check of the data chain (plan step A1).
#
# Counts every known inconsistency between the stored submissions, the
# outbox, the tall table, events and pivot tables, so that each repair step
# can show its numbers fall as expected and nothing else rises. It prints
# counts only: no names or other values leave the database.
#
# All checks run in one read-only transaction (repeatable read, so every
# count comes from the same snapshot), which is rolled back, as the
# read-only role pg_read_all_data. The database host may have little memory,
# so every statement runs with work_mem 8MB, no parallel workers, a 5-minute
# timeout and at most 1GB of temporary files, one statement at a time, and
# the payload walks run one template at a time.
#
# Staging by default. Production only with DATARUN_INTEGRITY_PRODUCTION=true.
#
# Exit codes: 2 usage or settings, 3 a guard refused to start, 4 psql failed
# (a query error, a timeout or a lost connection), 5 a line is missing or the
# output was not understood.

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
sql_dir="$repo_root/scripts/integrity/sql"
ssh_opts=(-o BatchMode=yes -o ServerAliveInterval=15 -o ServerAliveCountMax=8)
min_available_mb="${DATARUN_INTEGRITY_MIN_AVAILABLE_MB:-1024}"
min_free_disk_mb="${DATARUN_INTEGRITY_MIN_FREE_DISK_MB:-10240}"
# How long psql may run. Lower it to a few seconds to test the stop.
run_timeout_s="${DATARUN_INTEGRITY_TIMEOUT_S:-900}"
app_name="datarun-integrity-check"

# Every line the check prints. A missing one means a check did not run.
expected_keys=(
    1a 1b 1c 1d 1e 1f
    2a 2b 2c 2d
    3a 3b 3c 3d 3e
    4a 4b 4c 4d 4e 4f
    5a 5b 5c 5d
    6a 6b 6c
    7a 7b 7c 7d 7e
    8a 8b 8c 8d 8e 8f 8g 8h
    9a 9b 9c 9d
    10a 10b 10c 10d
    11a 11b 11c 11d 11e 11f 11g 11h
)

usage() {
    echo "Usage: $0 [--target staging|production] [--detail]" >&2
    exit 2
}

target="staging"
detail="false"
while [[ $# -gt 0 ]]; do
    case "$1" in
        --target) [[ $# -ge 2 ]] || usage; target="$2"; shift 2 ;;
        --detail) detail="true"; shift ;;
        *) usage ;;
    esac
done

case "$target" in
    staging)
        db_ssh="${DATARUN_STAGING_DB_SSH:-nmcp@product-staging-db.lab}"
        database="${DATARUN_STAGING_DATABASE:-datarun_staging}"
        ;;
    production)
        if [[ "${DATARUN_INTEGRITY_PRODUCTION:-}" != "true" ]]; then
            echo "Set DATARUN_INTEGRITY_PRODUCTION=true to connect to production." >&2
            exit 2
        fi
        db_ssh="${DATARUN_PRODUCTION_DB_SSH:-nmcp@datarun-prod-db.lab}"
        database="${DATARUN_PRODUCTION_DATABASE:-nmcpdb}"
        ;;
    *) usage ;;
esac
if [[ ! "$database" =~ ^[A-Za-z0-9_]+$ ]]; then
    echo "Unsupported database name: $database" >&2
    exit 2
fi
for setting in "$min_available_mb" "$min_free_disk_mb" "$run_timeout_s"; do
    if [[ ! "$setting" =~ ^[0-9]+$ ]]; then
        echo "DATARUN_INTEGRITY_MIN_AVAILABLE_MB, _MIN_FREE_DISK_MB and _TIMEOUT_S must be whole numbers." >&2
        exit 2
    fi
done

echo "Target: $target ($db_ssh, database $database), read only" >&2

refuse() {
    echo "$*; not running." >&2
    exit 3
}

# The command that runs psql on the database host as postgres, stopped after
# $1 seconds: SIGINT first, which cancels the running query and ends psql,
# then SIGKILL 30 seconds later. timeout runs inside sudo, as postgres, so its
# signals reach psql; outside sudo they would not. Every session is named
# $app_name, so it can be found in pg_stat_activity and cancelled.
psql_command() {
    local limit="$1"
    shift
    printf '%s' "sudo -n -u postgres timeout -s INT -k 30 $limit psql -X -q -A -t -v ON_ERROR_STOP=1 -d 'dbname=$database application_name=$app_name' $*"
}

# psql with its SQL on stdin.
remote_psql_stdin() {
    local limit="$1"
    shift
    ssh "${ssh_opts[@]}" "$db_ssh" "$(psql_command "$limit" "$@")"
}

# psql with nothing on stdin.
remote_psql() {
    local limit="$1"
    shift
    ssh -n "${ssh_opts[@]}" "$db_ssh" "$(psql_command "$limit" "$@")"
}

# Guard: memory. One heavy query once stopped the whole staging database
# (2026-11-23).
available_mb="$(ssh -n "${ssh_opts[@]}" "$db_ssh" \
    "awk '/^MemAvailable:/ { print int(\$2 / 1024) }' /proc/meminfo")" || available_mb=""
[[ "$available_mb" =~ ^[0-9]+$ ]] || refuse "Could not read the database host's memory"
(( available_mb >= min_available_mb )) \
    || refuse "The database host has $available_mb MB of memory available (minimum $min_available_mb)"

# Guard: disk. Sorts and hashes that pass work_mem spill to temporary files
# under the data directory.
data_dir="$(remote_psql 60 "-c 'SHOW data_directory'")" || data_dir=""
[[ "$data_dir" =~ ^/[A-Za-z0-9_./-]+$ ]] || refuse "Could not read the data directory"
free_disk_mb="$(ssh -n "${ssh_opts[@]}" "$db_ssh" \
    "df -Pk '$data_dir' | awk 'NR == 2 { print int(\$4 / 1024) }'")" || free_disk_mb=""
[[ "$free_disk_mb" =~ ^[0-9]+$ ]] || refuse "Could not read the free space under the data directory"
(( free_disk_mb >= min_free_disk_mb )) \
    || refuse "The data directory's disk has $free_disk_mb MB free (minimum $min_free_disk_mb)"

# Guard: exclusive locks and migrations. The check's read locks last until its
# rollback, so a deploy's DDL or a pivot swap would queue behind it, and the
# application behind that. Do not start while an exclusive lock is held or
# waited for on a table the check reads, or on a table not committed yet (the
# long CREATE TABLE ..._new AS of a pivot build; its OID matches no class we
# can see), or while Liquibase holds its changelog lock (a migration, even one
# paused between changesets).
lock_reading="$(remote_psql_stdin 60 <<'SQL'
BEGIN TRANSACTION READ ONLY;
SET LOCAL statement_timeout = '30s';
SET LOCAL lock_timeout = '5s';
SET LOCAL ROLE pg_read_all_data;
SELECT (SELECT count(*)
        FROM pg_locks l
        LEFT JOIN pg_class c ON c.oid = l.relation
        LEFT JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE l.locktype = 'relation' AND l.mode = 'AccessExclusiveLock'
          AND l.database = (SELECT oid FROM pg_database WHERE datname = current_database())
          AND (c.oid IS NULL OR n.nspname IN ('public', 'analytics', 'pivot', 'journal')))
       || ' ' ||
       (SELECT count(*) FROM public.databasechangeloglock WHERE locked);
ROLLBACK;
SQL
)" || lock_reading=""
[[ "$lock_reading" =~ ^[0-9]+\ [0-9]+$ ]] || refuse "Could not read the database's locks"
read -r exclusive_locks changelog_locked <<< "$lock_reading"
(( exclusive_locks == 0 )) \
    || refuse "$exclusive_locks exclusive locks are held or waited for on tables the check reads or on new tables (a deploy or a pivot build?)"
(( changelog_locked == 0 )) \
    || refuse "Liquibase holds its changelog lock (databasechangeloglock.locked): a migration is running or stopped half-way"

echo "Database host: $available_mb MB memory available, $free_disk_mb MB disk free, no exclusive locks, no migration lock" >&2

# The payload walks run one template at a time. List the templates first.
if ! listing="$(remote_psql_stdin 120 <<'SQL'
BEGIN TRANSACTION READ ONLY;
SET LOCAL statement_timeout = '1min';
SET LOCAL lock_timeout = '5s';
SET LOCAL ROLE pg_read_all_data;
SELECT DISTINCT template_uid FROM data_submission WHERE template_uid IS NOT NULL ORDER BY 1;
ROLLBACK;
SQL
)"; then
    echo "Listing the templates failed in psql; see the message above." >&2
    exit 4
fi
templates=()
while IFS= read -r t; do
    [[ -n "$t" ]] || continue
    [[ "$t" =~ ^[A-Za-z0-9]{11}$ ]] || refuse "Unexpected template uid in data_submission: $t"
    templates+=("$t")
done <<< "$listing"
(( ${#templates[@]} > 0 )) || refuse "No submission has a template"

run_id="$$-$(date +%s)"
remote_sql="/tmp/datarun-integrity-$run_id.sql"
local_sql="$(mktemp)"
local_out="$(mktemp)"

cleanup() {
    rm -f "$local_sql" "$local_out"
    ssh -n "${ssh_opts[@]}" "$db_ssh" "rm -f '$remote_sql'" >/dev/null 2>&1 || true
}
trap cleanup EXIT

# temp_file_limit may be set only by a superuser, so it comes before the
# switch to the read-only role.
{
    echo '\set ON_ERROR_STOP 1'
    echo 'BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY;'
    echo "SET LOCAL work_mem = '8MB';"
    echo "SET LOCAL max_parallel_workers_per_gather = 0;"
    echo "SET LOCAL statement_timeout = '5min';"
    echo "SET LOCAL lock_timeout = '5s';"
    echo "SET LOCAL idle_in_transaction_session_timeout = '2min';"
    echo "SET LOCAL jit = off;"
    echo "SET LOCAL client_connection_check_interval = '10s';"
    echo "SET LOCAL temp_file_limit = '1GB';"
    echo "SET LOCAL ROLE pg_read_all_data;"
    echo '\timing on'
    for f in "$sql_dir"/[0-9][0-9]-*.sql; do
        echo "\\echo @check $(basename "$f" .sql)"
        cat "$f"
    done
    echo '\echo @check payload-by-template'
    for t in "${templates[@]}"; do
        echo "\\set tmpl '$t'"
        echo "\\echo @template $t"
        cat "$sql_dir/payload-by-template.sql"
    done
    echo '\timing off'
    echo 'ROLLBACK;'
} > "$local_sql"

temp_stats() {
    remote_psql_stdin 60 <<'SQL' || true
SELECT temp_files || ' ' || temp_bytes FROM pg_stat_database WHERE datname = current_database();
SQL
}

scp -q "${ssh_opts[@]}" "$local_sql" "$db_ssh:$remote_sql"
ssh -n "${ssh_opts[@]}" "$db_ssh" "chmod 0644 '$remote_sql'"
temp_before="$(temp_stats)"
started="$(date +%s)"
status=0
remote_psql "$run_timeout_s" "-f '$remote_sql'" > "$local_out" || status=$?
if (( status == 124 || status == 137 )); then
    echo "psql was stopped after $run_timeout_s s (DATARUN_INTEGRITY_TIMEOUT_S); the check did not finish." >&2
    exit 4
elif (( status != 0 )); then
    echo "The check failed in psql (a query error or a lost connection); see the message above." >&2
    exit 4
fi
echo "Finished in $(( $(date +%s) - started )) s" >&2
temp_after="$(temp_stats)"
if [[ "$temp_before" =~ ^[0-9]+\ [0-9]+$ && "$temp_after" =~ ^[0-9]+\ [0-9]+$ ]]; then
    read -r files_before bytes_before <<< "$temp_before"
    read -r files_after bytes_after <<< "$temp_after"
    echo "Temporary files written in the database meanwhile (any session): $(( files_after - files_before )) files, $(( (bytes_after - bytes_before) / 1048576 )) MB" >&2
fi

# Result rows are "key|count|description[|scope]". "@check" and "@template"
# lines say which file and template produced the rows after them, and
# "Time:" lines come from \timing. The summary goes to stdout, one line per
# check in check order, so two runs can be compared with diff; timings go
# to stderr.
awk -v detail="$detail" -v expected="${expected_keys[*]}" '
    BEGIN { FS = "|"; sorter = "sort -V" }
    /^@check / { check = substr($0, 8); if (!(check in seen)) { seen[check] = 1; files[++nf] = check }; scope = ""; next }
    /^@template / { scope = substr($0, 11); next }
    /^Time: / { split($0, t, " "); secs[check] += t[2] / 1000; next }
    NF >= 3 && $1 ~ /^[0-9]+[a-z]$/ && $2 ~ /^-?[0-9]+$/ {
        if (!($1 in total)) label[$1] = $3
        total[$1] += $2
        where = (NF >= 4 && $4 != "") ? $4 : scope
        if (where != "" && $2 != 0) found[$1 "|" where] += $2
        next
    }
    NF > 0 { unexpected[++nu] = $0 }
    END {
        printf "%-4s %10s  %s\n", "chk", "count", "what it counts"
        for (k in total) printf "%-4s %10d  %s\n", k, total[k], label[k] | sorter
        close(sorter)
        if (detail == "true") {
            print ""
            printf "%-4s %10s  %s\n", "chk", "count", "where (template or pivot table)"
            for (kw in found) { split(kw, p, "|"); printf "%-4s %10d  %s\n", p[1], found[kw], p[2] | sorter }
            close(sorter)
        }
        for (i = 1; i <= nf; i++) printf "time %-24s %7.1f s\n", files[i], secs[files[i]] > "/dev/stderr"
        failed = 0
        for (i = 1; i <= nu; i++) { print "unexpected output: " unexpected[i] > "/dev/stderr"; failed = 1 }
        n = split(expected, keys, " ")
        for (i = 1; i <= n; i++) if (!(keys[i] in total)) { print "missing line: " keys[i] > "/dev/stderr"; failed = 1 }
        if (failed) exit 5
    }
' "$local_out"
