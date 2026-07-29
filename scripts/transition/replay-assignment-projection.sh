#!/usr/bin/env bash

set -euo pipefail

: "${DATARUN_ASSIGNMENT_PROJECTION_REPLAY:?Set DATARUN_ASSIGNMENT_PROJECTION_REPLAY=true to opt in}"
: "${DATARUN_ASSIGNMENT_PROJECTION_NO_WRITERS:?Set DATARUN_ASSIGNMENT_PROJECTION_NO_WRITERS=true after isolating assignment writers}"
: "${DATARUN_ASSIGNMENT_PROJECTION_REPLAY_MODE:?Set DATARUN_ASSIGNMENT_PROJECTION_REPLAY_MODE=validate or repair}"
: "${DATARUN_API_JAR:?Set DATARUN_API_JAR to the built DataRun API jar}"
: "${DATARUN_DB_URL:?Set DATARUN_DB_URL to the isolated clone JDBC URL}"
: "${DATARUN_DB_USERNAME:?Set DATARUN_DB_USERNAME to the disposable clone-only role}"
: "${DATARUN_DB_PASSWORD:?Set DATARUN_DB_PASSWORD for the disposable clone-only role}"

if [[ "$DATARUN_ASSIGNMENT_PROJECTION_REPLAY" != "true" ]]; then
    echo "DATARUN_ASSIGNMENT_PROJECTION_REPLAY must equal true." >&2
    exit 2
fi

if [[ "$DATARUN_ASSIGNMENT_PROJECTION_NO_WRITERS" != "true" ]]; then
    echo "DATARUN_ASSIGNMENT_PROJECTION_NO_WRITERS must equal true." >&2
    exit 2
fi

case "$DATARUN_ASSIGNMENT_PROJECTION_REPLAY_MODE" in
    validate|repair) ;;
    *)
        echo "DATARUN_ASSIGNMENT_PROJECTION_REPLAY_MODE must equal validate or repair." >&2
        exit 2
        ;;
esac

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
    --spring.main.banner-mode=off \
    --logging.level.ROOT=WARN \
    --logging.level.org.nmcpye.datarun=WARN \
    --logging.level.org.springframework.jdbc.core.JdbcTemplate=WARN \
    --logging.level.org.hibernate.orm.incubating=ERROR \
    --spring.liquibase.enabled=false \
    --application.liquibase.async-start=false \
    --datarun.scheduling.enabled=false \
    --datarun.assignment-shadow.replay.no-writers=true \
    --datarun.assignment-shadow.replay.isolated=true \
    --datarun.assignment-shadow.replay.enabled=true \
    --datarun.assignment-shadow.replay.mode="$DATARUN_ASSIGNMENT_PROJECTION_REPLAY_MODE"
