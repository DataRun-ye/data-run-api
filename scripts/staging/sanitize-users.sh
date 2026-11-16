#!/usr/bin/env bash

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
staging_db_ssh="${DATARUN_STAGING_DB_SSH:-nmcp@product-staging-db.lab}"
staging_database="${DATARUN_STAGING_DATABASE:-datarun_staging}"
remote_config_dir="/home/nmcp/.config/datarun-staging"
remote_config="$remote_config_dir/users.env"
remote_sql="/tmp/datarun-staging-sanitize-users.sql"

if [[ -n "${DATARUN_STAGING_FIELD_TEMPLATE_LOGIN:-}" ]] &&
   [[ ! "$DATARUN_STAGING_FIELD_TEMPLATE_LOGIN" =~ ^[A-Za-z0-9._@-]+$ ]]; then
    echo "DATARUN_STAGING_FIELD_TEMPLATE_LOGIN contains unsupported characters." >&2
    exit 2
fi

scp "$repo_root/deploy/staging/sanitize-users.sql" \
    "$staging_db_ssh:$remote_sql"

ssh "$staging_db_ssh" "
    set -eu
    cleanup() {
        rm -f '$remote_sql'
    }
    trap cleanup EXIT

    install -d -m 0700 '$remote_config_dir'

    if [ ! -s '$remote_config' ]; then
        if [ -z '${DATARUN_STAGING_FIELD_TEMPLATE_LOGIN:-}' ]; then
            echo 'Set DATARUN_STAGING_FIELD_TEMPLATE_LOGIN for the first staging-user setup.' >&2
            exit 2
        fi

        umask 0077
        {
            printf 'STAGING_ADMIN_LOGIN=staging-admin\\n'
            printf 'STAGING_ADMIN_PASSWORD=%s\\n' \"\$(openssl rand -hex 24)\"
            printf 'STAGING_FIELD_LOGIN=staging-field\\n'
            printf 'STAGING_FIELD_PASSWORD=%s\\n' \"\$(openssl rand -hex 24)\"
            printf 'STAGING_FIELD_TEMPLATE_LOGIN=%s\\n' \
                '${DATARUN_STAGING_FIELD_TEMPLATE_LOGIN}'
        } > '$remote_config'
    fi

    . '$remote_config'

    case \"\$STAGING_ADMIN_LOGIN:\$STAGING_FIELD_LOGIN:\$STAGING_FIELD_TEMPLATE_LOGIN\" in
        *[!A-Za-z0-9._@:-]*)
            echo 'Staging user configuration contains unsupported characters.' >&2
            exit 2
            ;;
    esac
    case \"\$STAGING_ADMIN_PASSWORD:\$STAGING_FIELD_PASSWORD\" in
        *[!0-9a-f:]*)
            echo 'Staging passwords must be generated hexadecimal values.' >&2
            exit 2
            ;;
    esac

    {
        printf '\\\\set staging_admin_login '\\''%s'\\''\\n' \
            \"\$STAGING_ADMIN_LOGIN\"
        printf '\\\\set staging_admin_password '\\''%s'\\''\\n' \
            \"\$STAGING_ADMIN_PASSWORD\"
        printf '\\\\set staging_field_login '\\''%s'\\''\\n' \
            \"\$STAGING_FIELD_LOGIN\"
        printf '\\\\set staging_field_password '\\''%s'\\''\\n' \
            \"\$STAGING_FIELD_PASSWORD\"
        printf '\\\\set staging_field_template_login '\\''%s'\\''\\n' \
            \"\$STAGING_FIELD_TEMPLATE_LOGIN\"
        cat '$remote_sql'
    } | sudo -n -u postgres psql \
        --dbname='$staging_database' >/dev/null

    sanitized_count=\$(sudo -n -u postgres psql \
        --dbname='$staging_database' \
        --tuples-only --no-align \
        --command=\"SELECT count(*) FROM app_user
            WHERE login IN ('staging-admin', 'staging-field')\")
    refresh_token_count=\$(sudo -n -u postgres psql \
        --dbname='$staging_database' \
        --tuples-only --no-align \
        --command='SELECT count(*) FROM refresh_token')

    if [ \"\$sanitized_count\" != '2' ] || [ \"\$refresh_token_count\" != '0' ]; then
        echo 'Staging identity verification failed.' >&2
        exit 1
    fi
"

echo "Staging identities sanitized and provisioned."
echo "Host-only staging credentials remain in $remote_config."
