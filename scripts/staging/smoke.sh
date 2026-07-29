#!/usr/bin/env bash

set -euo pipefail

base_url="${DATARUN_STAGING_BASE_URL:-https://staging.nmcpye.org}"

health="$(curl --fail --silent --show-error "$base_url/management/health")"
info="$(curl --fail --silent --show-error "$base_url/management/info")"

printf '%s\n' "$health"
printf '%s\n' "$info"

if [[ -n "${DATARUN_STAGING_LOGIN:-}" && -n "${DATARUN_STAGING_PASSWORD:-}" ]]; then
    login_body="$(
        jq -cn \
            --arg username "$DATARUN_STAGING_LOGIN" \
            --arg password "$DATARUN_STAGING_PASSWORD" \
            '{username: $username, password: $password}'
    )"
    token="$(
        printf '%s' "$login_body" |
        curl --fail --silent --show-error \
            -H 'Content-Type: application/json' \
            --data-binary @- \
            "$base_url/api/v1/authenticate" |
            jq -er '.accessToken'
    )"
    test -n "$token"
    curl --fail --silent --show-error \
        -H "Authorization: Bearer $token" \
        "$base_url/api/v1/myDetails" >/dev/null
    echo "Authenticated staging smoke passed."
else
    echo "Public health and build-identity smoke passed."
fi
