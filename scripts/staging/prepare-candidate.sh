#!/usr/bin/env bash

set -euo pipefail

: "${DATARUN_STAGING_ENV:?Set DATARUN_STAGING_ENV to the host-only staging .env path}"

set -a
source "$DATARUN_STAGING_ENV"
set +a

run_candidate() {
    docker run --rm --network host \
        -e SPRING_PROFILES_ACTIVE=prod,api-docs \
        -e SPRING_DATASOURCE_URL="$STAGING_DATABASE_URL" \
        -e SPRING_DATASOURCE_USERNAME="$STAGING_DATABASE_USERNAME" \
        -e SPRING_DATASOURCE_PASSWORD="$STAGING_DATABASE_PASSWORD" \
        -e SPRING_LIQUIBASE_URL="$STAGING_DATABASE_URL" \
        -e SPRING_LIQUIBASE_USER="$STAGING_DATABASE_USERNAME" \
        -e SPRING_LIQUIBASE_PASSWORD="$STAGING_DATABASE_PASSWORD" \
        -e DATARUN_JWT_BASE64_SECRET="$DATARUN_JWT_BASE64_SECRET" \
        "$DATARUN_API_IMAGE" \
        "$@"
}

common_args=(
    --spring.main.web-application-type=none
    --spring.main.lazy-initialization=true
    --spring.main.banner-mode=off
    --logging.level.ROOT=WARN
    --logging.level.org.nmcpye.datarun=WARN
    --application.liquibase.async-start=false
    --datarun.scheduling.enabled=false
)

echo "Applying candidate Liquibase migrations."
run_candidate "${common_args[@]}"

echo "Bootstrapping assignment facts and projection."
run_candidate \
    "${common_args[@]}" \
    --spring.liquibase.enabled=false \
    --datarun.assignment-shadow.bootstrap.isolated=true \
    --datarun.assignment-shadow.bootstrap.enabled=true

echo "Validating event-only assignment projection replay."
run_candidate \
    "${common_args[@]}" \
    --spring.liquibase.enabled=false \
    --datarun.assignment-shadow.replay.no-writers=true \
    --datarun.assignment-shadow.replay.isolated=true \
    --datarun.assignment-shadow.replay.enabled=true \
    --datarun.assignment-shadow.replay.mode=validate

echo "Candidate database preparation passed."
