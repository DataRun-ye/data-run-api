#!/usr/bin/env bash

set -euo pipefail

: "${DATARUN_STAGING_ENV:?Set DATARUN_STAGING_ENV to the host-only staging .env path}"

set -a
source "$DATARUN_STAGING_ENV"
set +a

active_container=""
candidate_env_file="$(mktemp /tmp/datarun-staging-candidate.XXXXXX.env)"
chmod 0600 "$candidate_env_file"

{
    printf 'JHIPSTER_SLEEP=0\n'
    printf 'SPRING_PROFILES_ACTIVE=prod\n'
    printf 'SPRING_DATASOURCE_URL=%s\n' "$STAGING_DATABASE_URL"
    printf 'SPRING_DATASOURCE_USERNAME=%s\n' "$STAGING_DATABASE_USERNAME"
    printf 'SPRING_DATASOURCE_PASSWORD=%s\n' "$STAGING_DATABASE_PASSWORD"
    printf 'SPRING_LIQUIBASE_URL=%s\n' "$STAGING_DATABASE_URL"
    printf 'SPRING_LIQUIBASE_USER=%s\n' "$STAGING_DATABASE_USERNAME"
    printf 'SPRING_LIQUIBASE_PASSWORD=%s\n' "$STAGING_DATABASE_PASSWORD"
    printf 'DATARUN_JWT_BASE64_SECRET=%s\n' "$DATARUN_JWT_BASE64_SECRET"
} > "$candidate_env_file"

cleanup() {
    if [[ -n "$active_container" ]]; then
        docker rm --force "$active_container" >/dev/null 2>&1 || true
    fi
    rm -f "$candidate_env_file"
}
trap cleanup EXIT INT TERM

run_candidate() {
    local phase="$1"
    shift
    active_container="datarun-staging-prepare-${phase}"
    docker rm --force "$active_container" >/dev/null 2>&1 || true

    timeout --foreground "${DATARUN_STAGING_PREPARE_TIMEOUT:-30m}" \
    docker run --rm --name "$active_container" --entrypoint /entrypoint.sh \
        --env-file "$candidate_env_file" \
        "$DATARUN_API_IMAGE" \
        "$@"

    active_container=""
}

run_migrations() {
    active_container="datarun-staging-prepare-migration"
    docker rm --force "$active_container" >/dev/null 2>&1 || true

    docker run --detach --name "$active_container" --entrypoint /entrypoint.sh \
        --env-file "$candidate_env_file" \
        "$DATARUN_API_IMAGE" \
        "${common_args[@]}" \
        --logging.level.org.nmcpye.datarun.DataRunApiApp=INFO \
        --logging.level.org.nmcpye.datarun.etl.admin.ScheduledOrchestratorRunner=INFO \
        >/dev/null

    local deadline=$((SECONDS + ${DATARUN_STAGING_MIGRATION_TIMEOUT_SECONDS:-900}))
    while (( SECONDS < deadline )); do
        local logs
        logs="$(docker logs "$active_container" 2>&1 || true)"
        if grep -Fq 'Started DataRunApiApp' <<<"$logs"; then
            if grep -Fq 'Checking for pending outbox events' <<<"$logs"; then
                echo "A scheduler ran during migration preparation." >&2
                return 1
            fi
            docker stop --time 30 "$active_container" >/dev/null 2>&1 || true
            docker rm --force "$active_container" >/dev/null 2>&1 || true
            active_container=""
            return 0
        fi
        if [[ "$(docker inspect --format '{{.State.Running}}' "$active_container" 2>/dev/null || true)" != "true" ]]; then
            tail -n 100 <<<"$logs" >&2
            return 1
        fi
        sleep 2
    done

    echo "Candidate migration startup timed out." >&2
    docker logs --tail 100 "$active_container" >&2 || true
    return 1
}

common_args=(
    --spring.main.web-application-type=none
    --spring.main.lazy-initialization=true
    --spring.main.banner-mode=off
    --logging.level.ROOT=WARN
    --logging.level.org.nmcpye.datarun=WARN
    --application.liquibase.async-start=false
    --datarun.scheduling.enabled=false
    --datarun.transition.capture-live-shadow-enabled=false
)

echo "Applying candidate Liquibase migrations."
run_migrations

echo "Bootstrapping assignment facts and projection."
run_candidate assignment-bootstrap \
    "${common_args[@]}" \
    --spring.liquibase.enabled=false \
    --datarun.assignment-shadow.bootstrap.isolated=true \
    --datarun.assignment-shadow.bootstrap.enabled=true

echo "Validating event-only assignment projection replay."
run_candidate assignment-replay \
    "${common_args[@]}" \
    --spring.liquibase.enabled=false \
    --datarun.assignment-shadow.replay.no-writers=true \
    --datarun.assignment-shadow.replay.isolated=true \
    --datarun.assignment-shadow.replay.enabled=true \
    --datarun.assignment-shadow.replay.mode=validate

echo "Candidate database preparation passed."
