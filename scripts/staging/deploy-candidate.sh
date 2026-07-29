#!/usr/bin/env bash

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$repo_root"

manifest="${DATARUN_STAGING_CANDIDATE_MANIFEST:-target/staging/candidate.env}"
staging_api_ssh="${DATARUN_STAGING_API_SSH:-nmcp@product-staging.lab}"
runtime_dir="/home/nmcp/datarun-staging"

if [[ ! -f "$manifest" ]]; then
    echo "Candidate manifest not found: $manifest" >&2
    exit 2
fi

scp \
    "$manifest" \
    deploy/staging/compose.yml \
    scripts/staging/prepare-candidate.sh \
    "$staging_api_ssh:/tmp/"

ssh "$staging_api_ssh" "
    set -eu
    install -d -m 0700 '$runtime_dir'
    install -m 0600 /tmp/candidate.env '$runtime_dir/candidate.env'
    install -m 0600 /tmp/compose.yml '$runtime_dir/compose.yml'
    install -m 0700 /tmp/prepare-candidate.sh '$runtime_dir/prepare-candidate.sh'
    rm -f /tmp/candidate.env /tmp/compose.yml /tmp/prepare-candidate.sh

    if [ ! -s '$runtime_dir/jwt-secret' ]; then
        umask 0077
        openssl rand -base64 64 | tr -d '\\n' > '$runtime_dir/jwt-secret'
    fi

    . '$runtime_dir/candidate.env'
    database_password=\$(cat '$runtime_dir/runtime-password')
    jwt_secret=\$(cat '$runtime_dir/jwt-secret')

    {
        printf 'DATARUN_API_IMAGE=%s\\n' \"\$DATARUN_API_IMAGE_PIN\"
        printf 'DATARUN_API_VERSION=%s\\n' \"\$DATARUN_API_VERSION\"
        printf 'DATARUN_API_COMMIT=%s\\n' \"\$DATARUN_API_COMMIT\"
        printf 'DATARUN_STAGING_BIND_ADDRESS=0.0.0.0\\n'
        printf 'DATARUN_STAGING_PORT=8080\\n'
        printf 'STAGING_DATABASE_URL=jdbc:postgresql://192.168.1.221:5432/datarun_staging?sslmode=require\\n'
        printf 'STAGING_DATABASE_USERNAME=datarun_staging\\n'
        printf 'STAGING_DATABASE_PASSWORD=%s\\n' \"\$database_password\"
        printf 'DATARUN_JWT_BASE64_SECRET=%s\\n' \"\$jwt_secret\"
    } > '$runtime_dir/.env'
    chmod 0600 '$runtime_dir/.env'

    cd '$runtime_dir'
    if ! docker image inspect \"\$DATARUN_API_IMAGE_PIN\" >/dev/null 2>&1; then
        docker pull \"\$DATARUN_API_IMAGE_PIN\" >/dev/null
    fi
    if ! DATARUN_STAGING_ENV='$runtime_dir/.env' \
        ./prepare-candidate.sh >prepare.log 2>&1; then
        tail -n 100 prepare.log >&2
        exit 1
    fi
    docker compose --env-file .env up -d
    docker compose --env-file .env ps
"
