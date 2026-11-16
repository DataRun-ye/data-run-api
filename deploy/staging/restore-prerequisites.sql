\set ON_ERROR_STOP on

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Production retains pg_idkit defaults only in the inactive supply_temp
-- schema. This compatibility function preserves those table definitions
-- without installing the retired extension.
--
-- Since the 2026-08-29 Proxmox move production carries this same function, so
-- the dump also contains it. refresh-from-production.sh filters that entry out
-- of the restore list; this definition remains the single owner.
CREATE OR REPLACE FUNCTION public.idkit_ulid_generate()
RETURNS text
LANGUAGE sql
VOLATILE
AS $function$
    SELECT upper(substr(encode(gen_random_bytes(16), 'hex'), 1, 26))
$function$;
