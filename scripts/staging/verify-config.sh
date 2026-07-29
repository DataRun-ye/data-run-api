#!/usr/bin/env bash

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$repo_root"

bash -n scripts/staging/*.sh

grep -Fq -- '--entrypoint /entrypoint.sh' scripts/staging/prepare-candidate.sh
grep -Fq -- '--env-file "$candidate_env_file"' scripts/staging/prepare-candidate.sh
grep -Fq -- 'timeout --foreground' scripts/staging/prepare-candidate.sh
grep -Fq -- 'up -d --wait --wait-timeout' scripts/staging/deploy-candidate.sh
grep -Fq -- '--datarun.transition.capture-live-shadow-enabled=false' \
    scripts/staging/prepare-candidate.sh
grep -Fq -- "ssh '\$production_refresh_alias'" \
    scripts/staging/refresh-from-production.sh
grep -Fq -- "mv -f '\$remote_partial_dump' '\$remote_dump'" \
    scripts/staging/refresh-from-production.sh
if grep -Fq -- 'ssh "$production_ssh"' \
    scripts/staging/refresh-from-production.sh; then
    echo "Production dumps must stream directly to the staging DB host." >&2
    exit 1
fi
if grep -Fq -- '--network host' scripts/staging/prepare-candidate.sh; then
    echo "Preparation containers must not use host networking." >&2
    exit 1
fi
if grep -Eq -- '-e .*PASSWORD=' scripts/staging/prepare-candidate.sh; then
    echo "Preparation secrets must not be passed in process arguments." >&2
    exit 1
fi

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
