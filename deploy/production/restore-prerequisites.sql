\set ON_ERROR_STOP on

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- The current database retains pg_idkit only for defaults in the inactive
-- supply_temp schema. PostgreSQL 16 on the target does not install pg_idkit.
-- Preserve those table definitions while public.generate_uid() remains the
-- active application-owned UID function.
CREATE OR REPLACE FUNCTION public.idkit_ulid_generate()
RETURNS text
LANGUAGE sql
VOLATILE
AS $function$
    SELECT upper(substr(encode(gen_random_bytes(16), 'hex'), 1, 26))
$function$;
