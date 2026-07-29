#!/usr/bin/env bash

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$repo_root"

bash -n scripts/staging/*.sh

compose_json="$(
    STAGING_DATABASE_USERNAME=verify \
    STAGING_DATABASE_PASSWORD=verify \
    DATARUN_JWT_BASE64_SECRET=verify \
        docker compose \
            --env-file deploy/staging/.env.example \
            -f deploy/staging/compose.yml \
            config --format json
)"

jq -e '
    .services.app.environment.DATARUN_SCHEDULING_ENABLED == "false" and
    .services.app.environment.DATARUN_TRANSITION_CAPTURE_LIVE_SHADOW_ENABLED == "false" and
    .services.app.environment.APPLICATION_LIQUIBASE_ASYNC_START == "false" and
    .services.app.environment.SPRING_DATASOURCE_URL
        == "jdbc:postgresql://192.168.1.221:5432/datarun_staging?sslmode=require" and
    .services.app.ports[0].host_ip == "0.0.0.0" and
    .services.app.ports[0].published == "8080"
' <<<"$compose_json" >/dev/null

echo "Staging configuration verification passed."
