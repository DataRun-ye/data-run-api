# API v2 Conventions

Role: the conventions every `/api/v2` module follows, and where each is enforced

Status: LIVING

v2 is a designed contract, not v1 renamed: its wire format is not the storage
format, and ordinary change happens inside v2 without a v3. The conventions
below are set once (data-chain step E0) and carried by every later module. No
v1 route, payload or error shape changes because of them.

Until the domain glossary lands (`docs/features/<name>/CONTEXT.md`), v2
infrastructure uses neutral names only; module names, codes and switches for
domain actions come from that glossary.

## Base And Naming

- Every route is under `/api/v2`; resource names are plural nouns in
  kebab-case path segments.
- JSON members and query parameters are camelCase.
- Times are RFC 3339 UTC instants with `Z`.

## Errors

Every v2 error response is an RFC 9457 problem, `application/problem+json`,
written by `web.v2.problem.V2ProblemResolver` (MVC) for every request under
`/api/v2` and declined for every other path, so v1's `ExceptionTranslator`
and its error shapes are untouched.

| Member | Content |
| --- | --- |
| `type` | `tag:nmcpye.org,2026:datarun/problem/<code>`: a permanent identifier that resolves nowhere (RFC 9457 allows it) |
| `title` | the code's fixed title |
| `status` | the HTTP status |
| `detail` | this occurrence in words; never a submitted value, a login or an exception message |
| `instance` | `urn:uuid:<request id>`, the id also sent as `X-Request-ID` and written in the request's log lines |
| `code` | one entry of the registry, `web.v2.problem.ProblemCode`; clients switch on it |
| `errors` | findings, each with a `code` and at most one locator: `pointer` (RFC 6901, into the body), `parameter`, `header`; `field` names a form field by id |

- Extension members (for example `currentVersion` on a 412) sit beside these;
  clients ignore members and codes they do not know.
- The registry is open: modules add codes; a published code is never renamed,
  repurposed or given another status.
- Status use: 400 malformed; 401; 403; 404; 405; 409 the same request may
  succeed later; 412 stale; 422 well formed but not applicable; 428 missing
  precondition; 5xx server faults.
- Every answer is one log line: INFO for 4xx, ERROR for 5xx (reported to
  GlitchTip), with method, route pattern, status, code, exception types and
  request id, and nothing the client sent.

## Authentication

- `/api/v2/**` has its own security chain, `web.v2.config.V2SecurityConfiguration`,
  ordered before the main chain and matching only v2 paths; the main chain
  and every v1 answer are unchanged.
- Every v2 route requires an authenticated caller. URL rules decide nothing
  finer; each module's own policy does, by action and scope, once that model
  is designed.
- The only credential is the bearer JWT from `POST /api/v1/authenticate`. The
  token names the account; the request's authority comes from the account's
  current database state. HTTP Basic is not accepted on v2.
- Refusals here are problems: 401 `unauthenticated` with the
  `WWW-Authenticate: Bearer` challenge, 403 `forbidden`. They are not logged
  by v2; the authentication listener records failures.

## Ids

- Every v2 id is an opaque string: the server's canonical id text, exactly as
  stored. It is never the 11-character `uid`, which stays a v1 alias for
  installed phones.
- Clients send ids back exactly as received and assume no format or length
  beyond 36 characters. The OpenAPI document says only `type: string`,
  `maxLength: 36`, no `format`. The server compares ids exactly.
- `web.v2.ids.ApiIds.isWellFormed` is the server's own guard (letters, digits,
  `-`, up to 36): a value no stored id can take names nothing, is answered as
  an unknown id, and never reaches a query or a log line. It is not part of
  the contract.
- Why opaque: stored ids are not all ULIDs, so no bit-for-bit uuid rule covers
  them, and a later move of the database to the uuid type must change nothing
  for clients. Measured on the staging production clone (2026-11-25, counts
  only): every character-typed `id` in the 24 tables of that kind holds a
  26-character ULID, except `data_template` (28 ULIDs, 27 24-hex legacy ids)
  and `ou_level` (4 one-character ids); `canonical_element.id` is a uuid
  column; `held_submission`, `go_live_rule` and the bookkeeping tables use
  bigint ids.

## Concurrency

- A versioned resource carries a strong `ETag`: its version in quotes, `"3"`
  (`web.v2.concurrency.EntityTag`), also shown in its body as `version`.
- Every command requires `If-Match` (`web.v2.concurrency.IfMatch`), compared
  by RFC 9110's strong comparison against the version its transaction has
  locked:
  - missing, or `*` (which names no version): 428 `precondition-required`;
  - not a list of quoted entity tags: 400 `invalid-if-match`;
  - no listed tag matches (a weak tag never does): 412 `precondition-failed`,
    with `currentVersion`, the current `ETag` and, when the module has one,
    `Link: <...>; rel="version-history"`.
- The comparison runs after the idempotency key is claimed and the resource's
  row is locked (Idempotency, below), never before.
