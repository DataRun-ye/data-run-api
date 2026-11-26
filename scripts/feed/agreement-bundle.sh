#!/usr/bin/env bash
# Builds the agreement harness as one bundle to run on a lab host (see scripts/feed/agreement.sh):
# target/feed-agreement.tar.gz, unpacking to feed-agreement/ with agreement.sh, the application's classes (classes/),
# the harness alone (harness/), its runtime libraries (lib/) and the oracle's SQL (sql/). It builds offline from the
# local Maven repository and touches no database.
set -euo pipefail
cd "$(dirname "$0")/../.."

out=target/feed-agreement
rm -rf "$out" target/feed-agreement.tar.gz
./mvnw -q -o -DskipTests -Dcheckstyle.skip -Djacoco.skip=true -Dmodernizer.skip=true \
    test-compile dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory="$out/lib"
mkdir -p "$out/classes" "$out/harness/org/nmcpye/datarun/submission/feed" "$out/sql"
cp -r target/classes/. "$out/classes/"
cp -r target/test-classes/org/nmcpye/datarun/submission/feed/agreement "$out/harness/org/nmcpye/datarun/submission/feed/"
cp scripts/feed/sql/oracle-walk.sql scripts/feed/sql/oracle-header.sql "$out/sql/"
cp scripts/feed/agreement.sh "$out/agreement.sh"
chmod +x "$out/agreement.sh"
tar -C target -czf target/feed-agreement.tar.gz feed-agreement
echo "target/feed-agreement.tar.gz"
