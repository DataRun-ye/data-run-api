#!/usr/bin/env bash

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$repo_root"

if [[ -n "$(git status --porcelain --untracked-files=all)" ]]; then
    echo "Release verification requires a clean working tree." >&2
    exit 1
fi

version="$(./mvnw -q -DforceStdout help:evaluate -Dexpression=project.version)"
case "$version" in
    ""|*SNAPSHOT*)
        echo "Release version must be a non-SNAPSHOT Maven version." >&2
        exit 1
        ;;
esac

commit="$(git rev-parse HEAD)"
short_commit="$(git rev-parse --short=12 HEAD)"

echo "Verifying DataRun API ${version} at ${short_commit}"
./mvnw -Pprod clean verify

jar_path="target/data-run-api-${version}.jar"
test -f "$jar_path"

build_version="$(
    unzip -p "$jar_path" META-INF/build-info.properties |
        sed -n 's/^build.version=//p'
)"
build_commit="$(
    unzip -p "$jar_path" BOOT-INF/classes/git.properties |
        sed -n 's/^git.commit.id.full=//p'
)"

if [[ "$build_version" != "$version" ]]; then
    echo "Built version ${build_version:-<missing>} does not match ${version}." >&2
    exit 1
fi

if [[ "$build_commit" != "$commit" ]]; then
    echo "Built commit ${build_commit:-<missing>} does not match ${commit}." >&2
    exit 1
fi

echo "Release verification passed: version=${version} commit=${commit}"
