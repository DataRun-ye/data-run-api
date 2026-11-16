\set ON_ERROR_STOP on

DO $block$
DECLARE
    runtime_password text := regexp_replace(
        pg_read_file('/etc/datarun-staging/runtime-password'),
        E'[\r\n]+$',
        ''
    );
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'datarun_staging') THEN
        EXECUTE format(
            'ALTER ROLE datarun_staging LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD %L',
            runtime_password
        );
    ELSE
        EXECUTE format(
            'CREATE ROLE datarun_staging LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD %L',
            runtime_password
        );
    END IF;
END
$block$;
