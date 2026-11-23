#!/usr/bin/env bash
# Import an extracted household CSV into reference_entry for one activity's
# villages, or with --undo delete that file's own entries again (only until a
# phone has synced those villages). Staging by default; dry run (rolled back)
# unless --apply.
#
# The CSV is uploaded first (compressed, private to the ssh user, removed
# afterwards), then loaded from that host-local copy, so a slow or stalled
# tunnel never holds a database transaction open.

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ssh_opts=(-o BatchMode=yes -o ServerAliveInterval=15 -o ServerAliveCountMax=8)
upload_timeout="${DATARUN_REFERENCE_UPLOAD_TIMEOUT:-900}"

usage() {
    echo "Usage: $0 --csv FILE --activity UID [--target staging|production] [--undo] [--apply]" >&2
    exit 2
}

csv=""
activity=""
target="staging"
apply="false"
action="load"
while [[ $# -gt 0 ]]; do
    case "$1" in
        --csv) csv="${2:-}"; shift 2 ;;
        --activity) activity="${2:-}"; shift 2 ;;
        --target) target="${2:-}"; shift 2 ;;
        --apply) apply="true"; shift ;;
        --undo) [[ "$action" == "load" ]] || usage; action="undo"; shift ;;
        *) usage ;;
    esac
done
sql_file="$repo_root/scripts/reference/$action-households.sql"

if [[ ! -s "$csv" ]]; then
    echo "CSV not found or empty: $csv" >&2
    exit 2
fi
if [[ ! "$activity" =~ ^[A-Za-z][A-Za-z0-9]{10}$ ]]; then
    echo "--activity must be an 11-character DataRun uid." >&2
    exit 2
fi

case "$target" in
    staging)
        db_ssh="${DATARUN_STAGING_DB_SSH:-nmcp@product-staging-db.lab}"
        database="${DATARUN_STAGING_DATABASE:-datarun_staging}"
        ;;
    production)
        if [[ "${DATARUN_REFERENCE_PRODUCTION:-}" != "true" ]]; then
            echo "Set DATARUN_REFERENCE_PRODUCTION=true to connect to production." >&2
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

mode="dry run"
[[ "$apply" == "true" ]] && mode="APPLY"
[[ "$action" == "undo" ]] && mode="UNDO, $mode"
echo "Target: $target ($db_ssh, database $database), activity $activity, $mode"

run_id="$$-$(date +%s)"
remote_data="\$HOME/.cache/datarun-reference/households-$run_id.csv.gz"
remote_sql="/tmp/datarun-reference-$action-$run_id.sql"

cleanup() {
    ssh "${ssh_opts[@]}" "$db_ssh" "rm -f $remote_data '$remote_sql'" >/dev/null 2>&1 || true
}
trap cleanup EXIT

echo "Uploading $(gzip -c "$csv" | wc -c) compressed bytes..."
timeout "$upload_timeout" ssh "${ssh_opts[@]}" "$db_ssh" \
    "umask 077 && mkdir -p \$HOME/.cache/datarun-reference && cat > $remote_data" \
    < <(gzip -c "$csv")
timeout "$upload_timeout" scp -q "${ssh_opts[@]}" "$sql_file" "$db_ssh:$remote_sql"

ssh "${ssh_opts[@]}" "$db_ssh" "
    set -eu
    chmod 0644 '$remote_sql'
    gunzip -c $remote_data | sudo -n -u postgres psql -X -q \
        -v ON_ERROR_STOP=1 \
        -v activity_uid='$activity' \
        -v apply='$apply' \
        -d '$database' \
        -f '$remote_sql'
"
