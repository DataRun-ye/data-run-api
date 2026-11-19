#!/usr/bin/env bash

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$repo_root"

# Builds a local image tar for the release gate. Nothing is pushed from here:
# a release promotes the rehearsed staging candidate by digest
# (docs/agent-context/release-boundary.md).
mode="${1:---tar}"
if [[ "$mode" != "--tar" ]]; then
    echo "Usage: $0 [--tar]" >&2
    echo "Releases promote the rehearsed staging candidate by digest; see docs/agent-context/release-boundary.md." >&2
    exit 1
fi

if [[ -n "$(git status --porcelain --untracked-files=all)" ]]; then
    echo "Image builds require a clean working tree." >&2
    exit 1
fi

version="$(./mvnw -q -DforceStdout help:evaluate -Dexpression=project.version)"
commit="$(git rev-parse HEAD)"
short_commit="$(git rev-parse --short=12 HEAD)"
repository="${DATARUN_IMAGE_REPOSITORY:-kaswarah/datarunapi}"
image_ref="${repository}:${version}-${short_commit}"

mkdir -p target/release
log_path="target/release/image-build.log"

./mvnw -Pprod -DskipTests verify \
    -Djib.to.image="$image_ref" \
    jib:buildTar | tee "$log_path"

manifest_path="target/release/${version}-${short_commit}.env"
{
    printf 'DATARUN_API_VERSION=%s\n' "$version"
    printf 'DATARUN_API_COMMIT=%s\n' "$commit"
    printf 'DATARUN_API_IMAGE=%s\n' "$image_ref"
} > "$manifest_path"

sha256sum target/jib-image.tar > target/release/jib-image.tar.sha256

echo "Image ready: ${image_ref}"
echo "Release manifest: ${manifest_path}"
