#!/usr/bin/env bash

set -euo pipefail

: "${DATARUN_STAGING_REFRESH:?Set DATARUN_STAGING_REFRESH=true to replace the staging database}"

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
restore_prerequisites="$repo_root/deploy/staging/restore-prerequisites.sql"

if [[ "$DATARUN_STAGING_REFRESH" != "true" ]]; then
    echo "DATARUN_STAGING_REFRESH must equal true." >&2
    exit 2
fi

production_ssh="${DATARUN_PRODUCTION_SSH:-hamza@api.nmcpye.org}"
staging_api_ssh="${DATARUN_STAGING_API_SSH:-nmcp@product-staging.lab}"
staging_db_ssh="${DATARUN_STAGING_DB_SSH:-nmcp@product-staging-db.lab}"
staging_database="${DATARUN_STAGING_DATABASE:-datarun_staging}"
remote_dump="/var/tmp/${staging_database}.dump"
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

if [[ "${DATARUN_STAGING_REUSE_DUMP:-false}" == "true" ]] &&
   ssh "$staging_db_ssh" \
       "test -s '$remote_dump' && pg_restore -l '$remote_dump' >/dev/null 2>&1"; then
    echo "Reusing the retained compressed staging dump."
else
    echo "Streaming a fresh compressed dump from production to the staging DB host."
    ssh "$production_ssh" \
        "docker exec nmcp-db sh -lc 'pg_dump -U \"\$POSTGRES_USER\" -d \"\$POSTGRES_DB\" -Fc --no-owner --no-privileges'" |
        ssh "$staging_db_ssh" "cat > '$remote_dump'"
fi

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
ssh "$staging_db_ssh" \
    "sudo -n -u postgres pg_restore --exit-on-error --no-owner --no-privileges \
        --role=datarun_staging --use-list='$remote_restore_list' \
        --dbname='$staging_database' '$remote_dump'"

refresh_complete=true
echo "Production refresh restored successfully."
