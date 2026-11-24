#!/usr/bin/env bash
# Read-only reconciliation check of the data chain (plan step A1).
#
# Counts every known inconsistency between the stored submissions, the
# outbox, the tall table, events and pivot tables, so that each repair step
# can show its numbers fall as expected and nothing else rises. It prints
# counts only: no names or other values leave the database.
#
# All queries run in one read-only transaction (repeatable read, so every
# count comes from the same snapshot), which is rolled back. The database
# host may have little memory, so every statement runs with work_mem 8MB,
# no parallel workers and a 5-minute timeout, one statement at a time, and
# the payload walks run one template at a time.
#
# Staging by default. Production only with DATARUN_INTEGRITY_PRODUCTION=true.

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
sql_dir="$repo_root/scripts/integrity/sql"
ssh_opts=(-o BatchMode=yes -o ServerAliveInterval=15 -o ServerAliveCountMax=8)
min_available_mb="${DATARUN_INTEGRITY_MIN_AVAILABLE_MB:-1024}"

usage() {
    echo "Usage: $0 [--target staging|production] [--detail]" >&2
    exit 2
}

target="staging"
detail="false"
while [[ $# -gt 0 ]]; do
    case "$1" in
        --target) target="${2:-}"; shift 2 ;;
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
if [[ ! "$min_available_mb" =~ ^[0-9]+$ ]]; then
    echo "DATARUN_INTEGRITY_MIN_AVAILABLE_MB must be a number of megabytes." >&2
    exit 2
fi

echo "Target: $target ($db_ssh, database $database), read only" >&2

# One heavy query once stopped the whole staging database (2026-11-23):
# refuse to start when the host is already short of memory.
available_mb="$(ssh "${ssh_opts[@]}" "$db_ssh" \
    "awk '/^MemAvailable:/ { print int(\$2 / 1024) }' /proc/meminfo")"
if [[ ! "$available_mb" =~ ^[0-9]+$ ]] || (( available_mb < min_available_mb )); then
    echo "The database host has ${available_mb:-unknown} MB available (minimum $min_available_mb); not running." >&2
    exit 3
fi
echo "Database host memory available: $available_mb MB" >&2

run_id="$$-$(date +%s)"
remote_sql="/tmp/datarun-integrity-$run_id.sql"
local_sql="$(mktemp)"
local_out="$(mktemp)"

cleanup() {
    rm -f "$local_sql" "$local_out"
    ssh "${ssh_opts[@]}" "$db_ssh" "rm -f '$remote_sql'" >/dev/null 2>&1 || true
}
trap cleanup EXIT

remote_psql() {
    ssh "${ssh_opts[@]}" "$db_ssh" \
        "sudo -n -u postgres psql -X -q -A -t -v ON_ERROR_STOP=1 -d '$database' $*"
}

# The payload walks run one template at a time. List the templates first.
templates="$(remote_psql <<'SQL'
BEGIN TRANSACTION READ ONLY;
SELECT DISTINCT template_uid FROM data_submission WHERE template_uid IS NOT NULL ORDER BY 1;
ROLLBACK;
SQL
)"
for t in $templates; do
    if [[ ! "$t" =~ ^[A-Za-z0-9]{11}$ ]]; then
        echo "Unexpected template uid in data_submission: $t" >&2
        exit 4
    fi
done

{
    echo '\set ON_ERROR_STOP 1'
    echo 'BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY;'
    echo "SET LOCAL work_mem = '8MB';"
    echo "SET LOCAL max_parallel_workers_per_gather = 0;"
    echo "SET LOCAL statement_timeout = '5min';"
    echo "SET LOCAL jit = off;"
    echo "SET LOCAL idle_in_transaction_session_timeout = '2min';"
    echo "SET LOCAL lock_timeout = '10s';"
    echo '\timing on'
    for f in "$sql_dir"/[0-9][0-9]-*.sql; do
        echo "\\echo @check $(basename "$f" .sql)"
        cat "$f"
    done
    echo '\echo @check payload-by-template'
    for t in $templates; do
        echo "\\set tmpl '$t'"
        echo "\\echo @template $t"
        cat "$sql_dir/payload-by-template.sql"
    done
    echo '\timing off'
    echo 'ROLLBACK;'
} > "$local_sql"

scp -q "${ssh_opts[@]}" "$local_sql" "$db_ssh:$remote_sql"
ssh "${ssh_opts[@]}" "$db_ssh" "chmod 0644 '$remote_sql'"
started="$(date +%s)"
remote_psql "-f '$remote_sql'" > "$local_out"
echo "Finished in $(( $(date +%s) - started )) s" >&2

# Result rows are "key|count|description[|scope]". "@check" and "@template"
# lines say which file and template produced the rows after them, and
# "Time:" lines come from \timing. The summary goes to stdout, one line per
# check in check order, so two runs can be compared with diff; timings go
# to stderr.
awk -v detail="$detail" '
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
        for (i = 1; i <= nu; i++) print "unexpected output: " unexpected[i] > "/dev/stderr"
        if (nu > 0) exit 5
    }
' "$local_out"
