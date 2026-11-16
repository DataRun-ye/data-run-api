#!/usr/bin/env bash

set -euo pipefail

base_url="${DATARUN_STAGING_BASE_URL:-https://staging.nmcpye.org}"
staging_db_ssh="${DATARUN_STAGING_DB_SSH:-nmcp@product-staging-db.lab}"
staging_identity="${DATARUN_STAGING_IDENTITY:-}"
remote_config="/home/nmcp/.config/datarun-staging/users.env"

health="$(curl --fail --silent --show-error "$base_url/management/health")"
info="$(curl --fail --silent --show-error "$base_url/management/info")"

printf '%s\n' "$health"
printf '%s\n' "$info"

smoke_identity() {
    local label="$1"
    local login="$2"
    local password="$3"
    local login_body
    local login_response
    local access_token
    local refresh_token
    local refresh_response
    local rotated_access_token
    local rotated_refresh_token

    login_body="$(
        jq -cn \
            --arg username "$login" \
            --arg password "$password" \
            '{username: $username, password: $password}'
    )"
    login_response="$(
        printf '%s' "$login_body" |
        curl --fail --silent --show-error \
            -H 'Content-Type: application/json' \
            --data-binary @- \
            "$base_url/api/v1/authenticate"
    )"
    access_token="$(jq -er '.accessToken' <<<"$login_response")"
    refresh_token="$(jq -er '.refreshToken' <<<"$login_response")"

    curl --fail --silent --show-error \
        -H "Authorization: Bearer $access_token" \
        "$base_url/api/v1/myDetails" >/dev/null

    refresh_response="$(
        jq -cn --arg refreshToken "$refresh_token" \
            '{refreshToken: $refreshToken}' |
        curl --fail --silent --show-error \
            -H 'Content-Type: application/json' \
            --data-binary @- \
            "$base_url/api/v1/refresh"
    )"
    rotated_access_token="$(jq -er '.accessToken' <<<"$refresh_response")"
    rotated_refresh_token="$(jq -er '.refreshToken' <<<"$refresh_response")"
    test "$rotated_refresh_token" != "$refresh_token"

    curl --fail --silent --show-error \
        -H "Authorization: Bearer $rotated_access_token" \
        "$base_url/api/v1/myDetails" >/dev/null

    echo "Authenticated staging smoke passed for $label."
}

if [[ -n "$staging_identity" ]]; then
    if [[ -n "${DATARUN_STAGING_LOGIN:-}" ||
          -n "${DATARUN_STAGING_PASSWORD:-}" ]]; then
        echo "Use a staging identity or explicit credentials, not both." >&2
        exit 2
    fi

    case "$staging_identity" in
        admin|field|all)
            ;;
        *)
            echo "DATARUN_STAGING_IDENTITY must be admin, field, or all." >&2
            exit 2
            ;;
    esac

    credentials="$(
        ssh "$staging_db_ssh" "
            set -eu
            . '$remote_config'
            printf 'admin\\t%s\\t%s\\n' \
                \"\$STAGING_ADMIN_LOGIN\" \"\$STAGING_ADMIN_PASSWORD\"
            printf 'field\\t%s\\t%s\\n' \
                \"\$STAGING_FIELD_LOGIN\" \"\$STAGING_FIELD_PASSWORD\"
        "
    )"

    while IFS=$'\t' read -r label login password; do
        if [[ "$staging_identity" == "all" ||
              "$staging_identity" == "$label" ]]; then
            smoke_identity "$label" "$login" "$password"
        fi
    done <<<"$credentials"
elif [[ -n "${DATARUN_STAGING_LOGIN:-}" &&
        -n "${DATARUN_STAGING_PASSWORD:-}" ]]; then
    smoke_identity \
        "explicit identity" \
        "$DATARUN_STAGING_LOGIN" \
        "$DATARUN_STAGING_PASSWORD"
elif [[ -n "${DATARUN_STAGING_LOGIN:-}" ||
        -n "${DATARUN_STAGING_PASSWORD:-}" ]]; then
    echo "Set both DATARUN_STAGING_LOGIN and DATARUN_STAGING_PASSWORD." >&2
    exit 2
else
    echo "Public health and build-identity smoke passed."
fi
