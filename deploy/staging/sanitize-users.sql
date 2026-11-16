\set ON_ERROR_STOP on

BEGIN;

SELECT set_config(
    'datarun.staging.admin_login',
    :'staging_admin_login',
    true
);
SELECT set_config(
    'datarun.staging.admin_password',
    :'staging_admin_password',
    true
);
SELECT set_config(
    'datarun.staging.field_login',
    :'staging_field_login',
    true
);
SELECT set_config(
    'datarun.staging.field_password',
    :'staging_field_password',
    true
);
SELECT set_config(
    'datarun.staging.field_template_login',
    :'staging_field_template_login',
    true
);

CREATE TEMPORARY TABLE staging_user_selection (
    account_type text PRIMARY KEY,
    user_id varchar(26) NOT NULL UNIQUE
) ON COMMIT DROP;

INSERT INTO staging_user_selection (account_type, user_id)
SELECT
    'field',
    app_user.id
FROM app_user
WHERE app_user.login IN (
    current_setting('datarun.staging.field_login'),
    current_setting('datarun.staging.field_template_login')
)
ORDER BY
    CASE
        WHEN app_user.login = current_setting('datarun.staging.field_login')
            THEN 0
        ELSE 1
    END
LIMIT 1;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM staging_user_selection
        WHERE account_type = 'field'
    ) THEN
        RAISE EXCEPTION
            'Staging field identity and configured template were not found';
    END IF;
END
$$;

INSERT INTO staging_user_selection (account_type, user_id)
SELECT
    'admin',
    app_user.id
FROM app_user
WHERE
    (
        app_user.login = current_setting('datarun.staging.admin_login')
        OR EXISTS (
            SELECT 1
            FROM app_user_authority
            WHERE
                app_user_authority.user_id = app_user.id
                AND app_user_authority.authority_name = 'ROLE_ADMIN'
        )
    )
    AND app_user.id <> (
        SELECT user_id
        FROM staging_user_selection
        WHERE account_type = 'field'
    )
ORDER BY
    CASE
        WHEN app_user.login = current_setting('datarun.staging.admin_login')
            THEN 0
        ELSE 1
    END,
    app_user.id
LIMIT 1;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM staging_user_selection
        WHERE account_type = 'admin'
    ) THEN
        RAISE EXCEPTION 'No distinct administrator identity exists in staging';
    END IF;
END
$$;

WITH blocked_credential AS (
    SELECT crypt(
        gen_random_uuid()::text || gen_random_uuid()::text,
        gen_salt('bf', 10)
    ) AS password_hash
)
UPDATE app_user
SET
    password_hash = blocked_credential.password_hash,
    email = NULL,
    mobile = NULL,
    activation_key = NULL,
    reset_key = NULL,
    reset_date = NULL
FROM blocked_credential;

DELETE FROM refresh_token;

UPDATE app_user
SET
    login = current_setting('datarun.staging.admin_login'),
    password_hash = crypt(
        current_setting('datarun.staging.admin_password'),
        gen_salt('bf', 10)
    ),
    first_name = 'Staging',
    last_name = 'Admin',
    activated = true,
    lang_key = 'en'
WHERE id = (
    SELECT user_id
    FROM staging_user_selection
    WHERE account_type = 'admin'
);

UPDATE app_user
SET
    login = current_setting('datarun.staging.field_login'),
    password_hash = crypt(
        current_setting('datarun.staging.field_password'),
        gen_salt('bf', 10)
    ),
    first_name = 'Staging',
    last_name = 'Field',
    activated = true,
    lang_key = 'en'
WHERE id = (
    SELECT user_id
    FROM staging_user_selection
    WHERE account_type = 'field'
);

DELETE FROM app_user_authority
WHERE user_id IN (
    SELECT user_id
    FROM staging_user_selection
);

INSERT INTO app_user_authority (user_id, authority_name)
SELECT user_id, 'ROLE_ADMIN'
FROM staging_user_selection
WHERE account_type = 'admin';

INSERT INTO app_user_authority (user_id, authority_name)
SELECT user_id, 'ROLE_USER'
FROM staging_user_selection
WHERE account_type = 'field';

DO $$
DECLARE
    unexpected_credential_count bigint;
    blocked_hash_count bigint;
    staging_hash_count bigint;
BEGIN
    SELECT count(*)
    INTO unexpected_credential_count
    FROM app_user
    WHERE
        email IS NOT NULL
        OR mobile IS NOT NULL
        OR reset_key IS NOT NULL
        OR activation_key IS NOT NULL;

    IF unexpected_credential_count <> 0 THEN
        RAISE EXCEPTION
            'Staging credential sanitization left % imported credential fields',
            unexpected_credential_count;
    END IF;

    IF (
        SELECT count(*)
        FROM app_user
        WHERE login IN (
            current_setting('datarun.staging.admin_login'),
            current_setting('datarun.staging.field_login')
        )
    ) <> 2 THEN
        RAISE EXCEPTION 'Staging identities were not provisioned exactly once';
    END IF;

    SELECT count(DISTINCT password_hash)
    INTO blocked_hash_count
    FROM app_user
    WHERE login NOT IN (
        current_setting('datarun.staging.admin_login'),
        current_setting('datarun.staging.field_login')
    );

    SELECT count(DISTINCT password_hash)
    INTO staging_hash_count
    FROM app_user
    WHERE login IN (
        current_setting('datarun.staging.admin_login'),
        current_setting('datarun.staging.field_login')
    );

    IF blocked_hash_count <> 1 OR staging_hash_count <> 2 THEN
        RAISE EXCEPTION
            'Staging password replacement failed (% blocked, % staging)',
            blocked_hash_count,
            staging_hash_count;
    END IF;
END
$$;

COMMIT;
