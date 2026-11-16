#!/usr/bin/env bash

set -euo pipefail

: "${DATARUN_STAGING_REFRESH:?Set DATARUN_STAGING_REFRESH=true to replace the staging database}"

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
restore_prerequisites="$repo_root/deploy/staging/restore-prerequisites.sql"

if [[ "$DATARUN_STAGING_REFRESH" != "true" ]]; then
    echo "DATARUN_STAGING_REFRESH must equal true." >&2
    exit 2
fi

staging_api_ssh="${DATARUN_STAGING_API_SSH:-nmcp@product-staging.lab}"
staging_db_ssh="${DATARUN_STAGING_DB_SSH:-nmcp@product-staging-db.lab}"
production_refresh_alias="${DATARUN_PRODUCTION_REFRESH_ALIAS:-datarun-production-refresh}"
staging_database="${DATARUN_STAGING_DATABASE:-datarun_staging}"
remote_dump="/var/tmp/${staging_database}.dump"
remote_partial_dump="${remote_dump}.partial"
remote_restore_list="/var/tmp/${staging_database}.restore.list"
refresh_complete=false

cleanup() {
    if [[ "$refresh_complete" == "true" ]]; then
        ssh "$staging_db_ssh" \
            "rm -f '$remote_dump' '$remote_restore_list'" >/dev/null 2>&1 || true
    else
        echo "Refresh failed; the compressed dump remains on the staging DB host for retry." >&2
    fi
}
trap cleanup EXIT

if [[ "${DATARUN_STAGING_REUSE_DUMP:-false}" == "true" ]] &&
   ssh "$staging_db_ssh" \
       "test -s '$remote_dump' && pg_restore -l '$remote_dump' >/dev/null 2>&1"; then
    echo "Reusing the retained compressed staging dump."
else
    echo "Streaming a fresh compressed dump directly from production to the staging DB host."
    ssh "$staging_db_ssh" "
        set -eu
        cleanup_partial_dump() {
            rm -f '$remote_partial_dump'
        }
        trap cleanup_partial_dump EXIT
        cleanup_partial_dump
        dump_started_at=\$(date +%s)
        ssh '$production_refresh_alias' \
            \"docker exec nmcp-db sh -lc \
                'pg_dump -U \\\"\\\$POSTGRES_USER\\\" -d \\\"\\\$POSTGRES_DB\\\" \
                    -Fc --no-owner --no-privileges'\" \
            > '$remote_partial_dump' &
        dump_pid=\$!
        while kill -0 \"\$dump_pid\" >/dev/null 2>&1; do
            sleep 10
            if kill -0 \"\$dump_pid\" >/dev/null 2>&1; then
                dump_bytes=\$(stat -c %s '$remote_partial_dump' 2>/dev/null || printf 0)
                dump_elapsed=\$((\$(date +%s) - dump_started_at))
                printf 'Dumping: %d MiB, elapsed %02d:%02d\\n' \
                    \$((dump_bytes / 1024 / 1024)) \
                    \$((dump_elapsed / 60)) \
                    \$((dump_elapsed % 60))
            fi
        done
        wait \"\$dump_pid\"
        pg_restore -l '$remote_partial_dump' >/dev/null
        mv -f '$remote_partial_dump' '$remote_dump'
        trap - EXIT
    "
fi

echo "Stopping the staging API."
ssh "$staging_api_ssh" "
    prepare_containers=\$(docker ps --all --quiet --filter name=datarun-staging-prepare-)
    if [ -n \"\$prepare_containers\" ]; then
        docker rm --force \$prepare_containers >/dev/null
    fi
    if [ -f /home/nmcp/datarun-staging/compose.yml ] &&
       [ -f /home/nmcp/datarun-staging/.env ]; then
        cd /home/nmcp/datarun-staging
        docker compose --env-file .env down
    fi
"

echo "Preparing the restore list."
ssh "$staging_db_ssh" \
    "pg_restore -l '$remote_dump' |
        grep -v -E ' EXTENSION - (pg_idkit|pgcrypto) | COMMENT - EXTENSION (pg_idkit|pgcrypto) ' \
        > '$remote_restore_list'"

echo "Replacing the staging database."
ssh "$staging_db_ssh" \
    "sudo -n -u postgres dropdb --if-exists --force '$staging_database' &&
     sudo -n -u postgres createdb --owner=datarun_staging '$staging_database'"
ssh "$staging_db_ssh" \
    "sudo -n -u postgres psql -d '$staging_database'" < "$restore_prerequisites"
ssh "$staging_db_ssh" "
    set -eu
    restore_started_at=\$(date +%s)
    sudo -n -u postgres pg_restore --exit-on-error --no-owner --no-privileges \
        --role=datarun_staging --use-list='$remote_restore_list' \
        --dbname='$staging_database' '$remote_dump' &
    restore_pid=\$!
    while kill -0 \"\$restore_pid\" >/dev/null 2>&1; do
        sleep 10
        if kill -0 \"\$restore_pid\" >/dev/null 2>&1; then
            restore_elapsed=\$((\$(date +%s) - restore_started_at))
            printf 'Restoring: elapsed %02d:%02d\\n' \
                \$((restore_elapsed / 60)) \
                \$((restore_elapsed % 60))
        fi
    done
    wait \"\$restore_pid\"
"

refresh_complete=true
echo "Production refresh restored successfully."
