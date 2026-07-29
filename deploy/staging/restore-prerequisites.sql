\set ON_ERROR_STOP on

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Production retains pg_idkit defaults only in the inactive supply_temp
-- schema. This staging-only compatibility function preserves those table
-- definitions without installing the retired extension.
CREATE OR REPLACE FUNCTION public.idkit_ulid_generate()
RETURNS text
LANGUAGE sql
VOLATILE
AS $function$
    SELECT upper(substr(encode(gen_random_bytes(16), 'hex'), 1, 26))
$function$;
