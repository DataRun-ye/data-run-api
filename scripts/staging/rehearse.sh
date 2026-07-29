#!/usr/bin/env bash

set -euo pipefail

: "${DATARUN_STAGING_REHEARSAL:?Set DATARUN_STAGING_REHEARSAL=true to replace and prepare staging}"

if [[ "$DATARUN_STAGING_REHEARSAL" != "true" ]]; then
    echo "DATARUN_STAGING_REHEARSAL must equal true." >&2
    exit 2
fi

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$repo_root"

scripts/staging/verify-config.sh
DATARUN_STAGING_REFRESH=true scripts/staging/refresh-from-production.sh
scripts/staging/deploy-candidate.sh
scripts/staging/smoke.sh

echo "Staging rehearsal passed. The environment remains running for manual smoke."
