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
| `errors` | findings, each with a `code` and at most one locator: `pointer` (a JSON Pointer into the body in RFC 9457's fragment form, `#/reason`), `parameter`, `header`; `field` names a form field by id |

- Extension members (for example `currentVersion` on a 412) sit beside these;
  clients ignore members and codes they do not know.
- Codes are kebab-case and name the problem, not the remedy
  (`search-too-broad`, `request-in-progress`); the generic ones keep their
  HTTP status's name. A 409 never carries a bare `conflict` code.
- The registry is open: modules add codes with their glossary's words; a
  published code is never renamed, repurposed or given another status.
- The problem's `status` member is RFC 9457's HTTP status; no domain field or
  parameter is ever a bare `status` (vocabulary rule R9).
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

## Paging

- Lists use keyset paging (`web.v2.paging`): `limit` (default 50, 1 to 200,
  otherwise 400 `bad-request`) and an opaque `cursor`; a handler takes
  `PageQuery` as a parameter.
- The answer is `{"items": [...], "next": <cursor or null>}`; `next` is always
  present. A GET also sends `Link: <...>; rel="next"`, a relative reference to
  the same request with the cursor replaced. There are no totals.
- A cursor is base64url text holding the sort key of the last item shown and
  a binding to its query (`QueryBinding`: the list's name and its filters,
  order-insensitive; the limit may change between pages). A cursor of another
  query, or anything else, is 400 `invalid-cursor`, never repeated back. It is
  not signed: it holds sort keys only, and the query after it still applies
  the caller's filters and access.
- A query reads `limit + 1` rows after the position, ordered by a unique key;
  `Keyset.page` turns them into the page and its next cursor.

## Caller

- A v2 handler that needs to know who is asking takes `web.v2.caller.ApiCaller`:
  the account's durable user id and its current authorities, as the v2 chain
  loaded them for this request; commands receive it in their context.
- It is the seam for access decisions, not a decision. No permission for
  changing data is wired in v2 yet: operations divide work by action
  (monitoring, auditing and reviewing, making changes, feedback), so the model
  will be per action and scoped by activity, designed after the glossary.
- Records name the caller by user id, never by login.

## Methods And Actions

The vocabulary study's API rules (`.review/data-chain/2026-11-25-vocabulary.md`
§6) apply; the domain nouns and verbs come from the feature's glossary.

- Reads are `GET`. A change is a named action, never `PUT` or `PATCH` on a
  submission: `POST /api/v2/<resources>/{id}:<verb>` (AIP-136), verbs in
  camelCase; actions on a collection are `POST /api/v2/<resources>:<verb>`,
  such as `:search`, a safe read with a large filter body.
- Spring MVC routes this form cleanly (`V2ActionRoutingIT`, real HTTP through
  Undertow, the firewall and the v2 chain): ULID, 24-hex and UUIDv7-text ids
  followed by `:verb` reach their action with the id intact, whether the colon
  arrives as is (as nginx passes it) or as `%3A`/`%3a`; a sub-collection
  (`/{id}/notes`) and the read (`/{id}`) route beside the actions.
- Map a controller at the parent path and write the collection into each
  method's pattern: a method mapping `":search"` under a class mapped at the
  collection combines to `/<resources>/:search`.
- A verb no route declares reaches no action: the read's `{id}` matches the
  path with the verb inside the id, so a `POST` answers 405 (`Allow: GET`) and
  a `GET` hands the read an id with a colon, which the id guard answers as
  unknown. Ids never contain a colon.
- Enumeration values are UPPER_SNAKE; JSON members, query parameters, verbs
  and operation names camelCase; problem codes kebab-case.
- Targets are named by id only (`field`, `row`, `destination`), never by name,
  section path or array index. Nothing is meant by null or by omission:
  clearing is its own operation, and whatever a request leaves out is
  untouched.
- `reason` is only ever a person's text; codes the server assigns are `cause`.
