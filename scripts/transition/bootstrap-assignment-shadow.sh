#!/usr/bin/env bash

set -euo pipefail

: "${DATARUN_ASSIGNMENT_SHADOW_BOOTSTRAP:?Set DATARUN_ASSIGNMENT_SHADOW_BOOTSTRAP=true to opt in}"
: "${DATARUN_API_JAR:?Set DATARUN_API_JAR to the built DataRun API jar}"
: "${DATARUN_DB_URL:?Set DATARUN_DB_URL to the isolated clone JDBC URL}"
: "${DATARUN_DB_USERNAME:?Set DATARUN_DB_USERNAME to the disposable clone-only role}"
: "${DATARUN_DB_PASSWORD:?Set DATARUN_DB_PASSWORD for the disposable clone-only role}"

if [[ "$DATARUN_ASSIGNMENT_SHADOW_BOOTSTRAP" != "true" ]]; then
    echo "DATARUN_ASSIGNMENT_SHADOW_BOOTSTRAP must equal true." >&2
    exit 2
fi

if [[ ! -f "$DATARUN_API_JAR" ]]; then
    echo "DATARUN_API_JAR does not identify a readable file." >&2
    exit 2
fi

export SPRING_DATASOURCE_URL="$DATARUN_DB_URL"
export SPRING_DATASOURCE_USERNAME="$DATARUN_DB_USERNAME"
export SPRING_DATASOURCE_PASSWORD="$DATARUN_DB_PASSWORD"
DATARUN_JWT_BASE64_SECRET="$(openssl rand -base64 64)"
export DATARUN_JWT_BASE64_SECRET

exec java -jar "$DATARUN_API_JAR" \
    "$@" \
    --spring.main.web-application-type=none \
    --spring.main.lazy-initialization=true \
    --spring.liquibase.enabled=false \
    --application.liquibase.async-start=false \
    --datarun.scheduling.enabled=false \
    --datarun.assignment-shadow.bootstrap.isolated=true \
    --datarun.assignment-shadow.bootstrap.enabled=true
