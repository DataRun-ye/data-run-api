#!/usr/bin/env bash

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$repo_root"

mode="${1:---tar}"
if [[ "$mode" != "--tar" && "$mode" != "--push" ]]; then
    echo "Usage: $0 [--tar|--push]" >&2
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

if [[ "$mode" == "--push" ]]; then
    branch="$(git branch --show-current)"
    if [[ "$branch" != "main" ]]; then
        echo "Publishing requires the main branch; current branch is ${branch:-detached}." >&2
        exit 1
    fi

    if ! git tag --points-at HEAD | grep -Fxq "v${version}"; then
        echo "Publishing requires tag v${version} on HEAD." >&2
        exit 1
    fi
fi

mkdir -p target/release
log_path="target/release/image-build.log"

goal="jib:buildTar"
if [[ "$mode" == "--push" ]]; then
    goal="jib:build"
fi

./mvnw -Pprod -DskipTests verify \
    -Djib.to.image="$image_ref" \
    "$goal" | tee "$log_path"

manifest_path="target/release/${version}-${short_commit}.env"
{
    printf 'DATARUN_API_VERSION=%s\n' "$version"
    printf 'DATARUN_API_COMMIT=%s\n' "$commit"
    printf 'DATARUN_API_IMAGE=%s\n' "$image_ref"
} > "$manifest_path"

if [[ "$mode" == "--tar" ]]; then
    sha256sum target/jib-image.tar > target/release/jib-image.tar.sha256
else
    digest="$(sed -n 's/^\[INFO\] Digest: \(sha256:[0-9a-f]\{64\}\)$/\1/p' "$log_path" | tail -1)"
    if [[ -z "$digest" ]]; then
        echo "Jib did not report the pushed image digest." >&2
        exit 1
    fi
    printf 'DATARUN_API_IMAGE_DIGEST=%s\n' "$digest" >> "$manifest_path"
fi

echo "Image ready: ${image_ref}"
echo "Release manifest: ${manifest_path}"
