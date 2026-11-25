#!/usr/bin/env bash
# The feed's agreement harness (build step F8): the submission normalizer against the feed spike's oracle, on every
# head of the revision journal of the database named, counts only. See FeedAgreement
# (src/test/java/org/nmcpye/datarun/submission/feed/agreement) for what it compares.
#
# Usage:
#   FEED_AGREEMENT_SETTINGS=/path/outside/the/repository/agreement.env scripts/feed/agreement.sh
#
# The settings file is sourced, never printed, and sets:
#   FEED_AGREEMENT_JDBC_URL    jdbc:postgresql://<host>:<port>/<database> of a restored clone; a host whose name says
#                              production is refused
#   FEED_AGREEMENT_USER        a clone-only role that can read public and journal
#   FEED_AGREEMENT_PASSWORD
#   FEED_AGREEMENT_FORMS       optional: form uids, comma-separated; every form when unset
#   FEED_AGREEMENT_BATCH       optional: heads per batch, 500 when unset
#
# It reads in one REPEATABLE READ READ ONLY transaction, with work_mem 8MB, no parallel workers, no JIT and a
# 300 s statement timeout, and writes nothing. Before running it on a shared server, check its memory, disk and WAL
# as scripts/integrity/check.sh does.
#
# The counts go to stdout and to target/feed-agreement.txt. Exit 0 when every "agree:" line reads 0, 1 when one does
# not, 2 for a usage error, anything else when Maven or the database failed.
set -euo pipefail
cd "$(dirname "$0")/../.."

settings="${FEED_AGREEMENT_SETTINGS:-}"
if [ -z "$settings" ] || [ ! -r "$settings" ]; then
    echo "Set FEED_AGREEMENT_SETTINGS to a readable settings file (see the header of $0)" >&2
    exit 2
fi
set -a
# shellcheck disable=SC1090
. "$settings"
set +a
if [ -z "${FEED_AGREEMENT_JDBC_URL:-}" ]; then
    echo "The settings file sets no FEED_AGREEMENT_JDBC_URL" >&2
    exit 2
fi

rm -f target/feed-agreement.txt
status=0
./mvnw -q -Dtest=FeedAgreementRun -Dsurefire.failIfNoSpecifiedTests=false -Djacoco.skip=true test || status=$?
if [ -r target/feed-agreement.txt ]; then
    cat target/feed-agreement.txt
    if [ "$status" -ne 0 ]; then
        exit 1
    fi
fi
exit "$status"
