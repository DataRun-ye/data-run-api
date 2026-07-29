#!/usr/bin/env bash

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$repo_root"

if [[ -n "$(git status --porcelain --untracked-files=all)" ]]; then
    echo "Candidate publication requires a clean working tree." >&2
    exit 2
fi

version="$(./mvnw -q -DforceStdout help:evaluate -Dexpression=project.version)"
commit="$(git rev-parse HEAD)"
short_commit="$(git rev-parse --short=12 HEAD)"
repository="${DATARUN_IMAGE_REPOSITORY:-kaswarah/datarunapi}"
image_ref="${repository}:${version}-staging-${short_commit}"

mkdir -p target/staging
verify_log="target/staging/verify.log"
image_log="target/staging/image.log"

scripts/staging/verify-config.sh

echo "Running the release gate; full output is in $verify_log."
if ! scripts/release/verify.sh >"$verify_log" 2>&1; then
    tail -n 80 "$verify_log" >&2
    exit 1
fi

echo "Publishing immutable staging candidate $image_ref."
if ! ./mvnw -Pprod -DskipTests \
    -Djib.to.image="$image_ref" \
    jib:build >"$image_log" 2>&1; then
    tail -n 80 "$image_log" >&2
    exit 1
fi

digest="$(tr -d '\r\n' < target/jib-image.digest)"
if [[ ! "$digest" =~ ^sha256:[0-9a-f]{64}$ ]]; then
    echo "Jib did not report a valid image digest." >&2
    exit 1
fi

manifest="target/staging/candidate.env"
{
    printf 'DATARUN_API_VERSION=%s\n' "$version"
    printf 'DATARUN_API_COMMIT=%s\n' "$commit"
    printf 'DATARUN_API_IMAGE=%s\n' "$image_ref"
    printf 'DATARUN_API_IMAGE_DIGEST=%s\n' "$digest"
    printf 'DATARUN_API_IMAGE_PIN=%s@%s\n' "$repository" "$digest"
} >"$manifest"

echo "Candidate published: $image_ref"
echo "Candidate manifest: $manifest"
