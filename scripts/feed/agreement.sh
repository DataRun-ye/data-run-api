#!/usr/bin/env bash
# The feed's agreement harness (build step F8): the submission normalizer against the feed spike's oracle, on every
# head of the revision journal of the database named, counts only. See FeedAgreement
# (src/test/java/org/nmcpye/datarun/submission/feed/agreement) for what it compares.
#
# It runs on a lab host, detached, never through a pipe or tunnel from another machine (owner, 2026-11-26). On a build
# machine, scripts/feed/agreement-bundle.sh makes target/feed-agreement.tar.gz: this script, the application's
# classes, the harness, its runtime libraries and the oracle's SQL. Copy it and a settings file to the host, unpack it,
# and start it there:
#
#   tar -xzf feed-agreement.tar.gz && cd feed-agreement
#   FEED_AGREEMENT_SETTINGS=/path/outside/the/bundle/agreement.env setsid -f ./agreement.sh > run.log 2>&1 < /dev/null
#
# then fetch feed-agreement.txt (the counts) and run.log. It needs Java 17 or later (JAVA names another java, such as
# one in a container).
#
# The settings file is sourced, never printed, and sets:
#   FEED_AGREEMENT_JDBC_URL    jdbc:postgresql://<host>:<port>/<database> of a restored clone
#   FEED_AGREEMENT_DATABASE    the name of that database: the run refuses unless it is connected to exactly it
#   FEED_AGREEMENT_USER        a clone-only role that can read public and journal
#   FEED_AGREEMENT_PASSWORD
#   FEED_AGREEMENT_FORMS       optional: form ids, comma-separated, "-" for heads whose form is not stored;
#                              every form when unset
#   FEED_AGREEMENT_BATCH       optional: heads per batch, 500 when unset
#   FEED_AGREEMENT_REPORT      optional: where the counts go, feed-agreement.txt when unset
#
# Production's database host (datarun-prod-db.lab, 192.168.1.111) and database (nmcpdb) are refused unless
# FEED_AGREEMENT_PRODUCTION=true, as scripts/integrity/check.sh gates production; a clone restored under the
# production name needs it too.
#
# It reads in one REPEATABLE READ READ ONLY transaction, with work_mem 8MB, no parallel workers, no JIT and a
# 300 s statement timeout, and writes nothing. Before running it on a shared server, check its memory, disk and WAL
# as scripts/integrity/check.sh does.
#
# Exit 0 when every "agree:" line reads 0, 1 when one does not, 2 when the settings refuse the run, 3 when the
# database or the run failed.
set -euo pipefail
cd "$(dirname "$0")"

settings="${FEED_AGREEMENT_SETTINGS:-}"
if [ -z "$settings" ] || [ ! -r "$settings" ]; then
    echo "Set FEED_AGREEMENT_SETTINGS to a readable settings file (see the header of $0)" >&2
    exit 2
fi
if [ ! -d classes ] || [ ! -d harness ] || [ ! -d lib ] || [ ! -d sql ]; then
    echo "Run this from the unpacked bundle that scripts/feed/agreement-bundle.sh builds" >&2
    exit 2
fi
set -a
# shellcheck disable=SC1090
. "$settings"
set +a

# Out of memory, the JVM exits 3, as any other failure of the run does, rather than going on half alive.
exec "${JAVA:-java}" -XX:+ExitOnOutOfMemoryError -Dfeed.agreement.sql=sql -cp "classes:harness:lib/*" \
    org.nmcpye.datarun.submission.feed.agreement.FeedAgreementMain
