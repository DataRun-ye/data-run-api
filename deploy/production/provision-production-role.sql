\set ON_ERROR_STOP on

DO $block$
DECLARE
    runtime_password text := regexp_replace(
        pg_read_file('/etc/datarun-production/runtime-password'),
        E'[\r\n]+$',
        ''
    );
BEGIN
    IF runtime_password = '' THEN
        RAISE EXCEPTION 'The production runtime password file is empty';
    END IF;

    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'nmcpdb') THEN
        EXECUTE format(
            'ALTER ROLE nmcpdb LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD %L',
            runtime_password
        );
    ELSE
        EXECUTE format(
            'CREATE ROLE nmcpdb LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD %L',
            runtime_password
        );
    END IF;
END
$block$;
