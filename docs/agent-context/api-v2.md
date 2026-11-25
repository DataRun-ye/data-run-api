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
and its error shapes are untouched. What a v2 request is has one definition,
`ApiV2.isV2`, used by the resolver and the security chain alike: the decoded
path within the application, as MVC routes it, so `/api/%762/x` is v2 for
all of them.

| Member | Content |
| --- | --- |
| `type` | `tag:nmcpye.org,2026:datarun/problem/<code>`: a permanent identifier that resolves nowhere (RFC 9457 allows it) |
| `title` | the code's fixed title |
| `status` | the HTTP status |
| `detail` | this occurrence in words; never a submitted value, a login or an exception message (validation findings use fixed text per constraint, never a constraint's or a binder's own message) |
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
  parameter is ever a bare `status` (naming rule R9 of
  [submission review](../features/submission-review/spec.md#9-api-conventions)).
- Status use: 400 malformed; 401; 403; 404; 405; 409 the same request may
  succeed later; 412 stale; 422 well formed but not applicable; 428 missing
  precondition; 5xx server faults.
- Every answer is one log line: INFO for 4xx, ERROR for 5xx, with method,
  route pattern, status, code, exception types (for a 5xx 6.5.5's
  `FailureShape`: types, SQLState, first application frames) and request id,
  and nothing the client sent. The ERROR line takes `ExceptionTranslator`'s
  arguments in the same order, so GlitchTip groups v2 faults by the same
  fingerprint (route, outermost type, SQLState).

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
  `WWW-Authenticate: Bearer` challenge, 403 `forbidden`. They log nothing: a
  missing token is the client's, and since 6.5.5 a rejected bearer token is
  no failed login and leaves no line.

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
- A resource whose view also shows another resource's state carries an
  opaque strong `ETag` that changes when either changes, and no `version`
  member. Clients never parse it. The one such resource so far is a
  submission's conflict (`docs/features/submission-review/spec.md`).
- Every command requires `If-Match` (`web.v2.concurrency.IfMatch`), compared
  by RFC 9110's strong comparison against the version its transaction has
  locked:
  - missing, or `*` (which names no version): 428 `precondition-required`;
  - not a list of quoted entity tags: 400 `invalid-if-match`;
  - no listed tag matches (a weak tag never does): 412 `precondition-failed`,
    with `currentVersion`, the current `ETag` and, when the module has one,
    `Link: <...>; rel="version-history"`.
- The comparison runs after the idempotency key is claimed and the resource's
  row is locked (Changing Requests, below), never before.
- Entity tags must reach clients strong. The application's own compression
  (production profile: `application/json` from 1 KB) keeps them strong
  (`V2CompressionIT`), but nginx's gzip module turns a strong `ETag` weak, and
  `If-Match` then never matches. Deployment rule: the proxy must not gzip
  `/api/v2` JSON (`release-boundary.md`).

## Paging

- Lists use keyset paging (`web.v2.paging`): `limit` (default 50, 1 to 200,
  otherwise 400 `bad-request`) and an opaque `cursor`; a handler takes
  `PageQuery` as a parameter.
- The answer is `{"items": [...], "next": <cursor or null>}`; `next` is always
  present. A GET also sends `Link: <...>; rel="next"`, a relative reference to
  the same request with the cursor replaced; characters a URI cannot hold
  literally (a raw `[`, `|`, space or non-ASCII character) are percent-encoded,
  so the link is always valid. There are no totals.
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

The API naming rules of [submission review's specification](../features/submission-review/spec.md#9-api-conventions)
(§9, R1 to R15) apply; the domain nouns and verbs come from each feature's
glossary under `docs/features/`.

- Reads are `GET`; standard creates are `POST` on a collection and revokes
  `DELETE`. A change is a named action, never `PUT` or `PATCH`:
  `POST /api/v2/<resources>/{id}:<verb>` (AIP-136), verbs in camelCase;
  actions on a collection are `POST /api/v2/<resources>:<verb>`, such as
  `:search`, a safe read with a large filter body.
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

## Changing Requests

Every changing method takes the same four things (naming rule R4 of
[submission review](../features/submission-review/spec.md#9-api-conventions)): the
`If-Match` and `Idempotency-Key` headers, which E0 enforces through
`CommandRequest`, and a required `reason` with an optional `dryRun` in its
body, which each method's request defines; E0 builds no shared body type.

- A handler becomes a command by taking `web.v2.idempotency.CommandRequest`:
  before it runs, no caller is 401, a missing key 400
  `idempotency-key-missing`, a key that is not one UUID 400
  `invalid-idempotency-key`, no `If-Match` or `*` 428, a malformed one 400.
  Keys are quoted (`"8e03978e-..."`, the Structured Field string of the IETF
  draft-09) or bare; the nil and max UUIDs are refused.
- `IdempotentCommands.execute` runs it in one READ COMMITTED transaction, in
  this order: bound lock waits (`datarun.v2.commands.lock-timeout`, 5 s);
  claim the caller's key in `api.idempotency_key`; the command locks its row
  first (`SELECT ... FOR NO KEY UPDATE`, or `FOR UPDATE` when the command
  changes a key column), checks `If-Match` through its context,
  applies the change; the outcome is written on the key; commit.
  - A duplicate waits on the key, never on the row, and then gets the first
    outcome. Past the lock timeout it is 409 `request-in-progress`; a row held
    past it is 409 `resource-busy`.
  - The same key with the same request is a replay, even once its `If-Match`
    is stale: the first status and body, the `ETag`, and
    `Idempotent-Replayed: true`. With a different request it is 422
    `idempotency-key-reused`.
  - Any refusal or failure rolls back the command with its claim: only
    successes are remembered, and a retry of a refused request runs again.
  - The contract is enforced: a change whose command never called
    `checkVersion` fails (500, rolled back), and work in a transaction of its
    own (REQUIRES_NEW, NOT_SUPPORTED), which would commit outside the claim,
    fails before it starts.
- The request's fingerprint is a SHA-256 of the method, the decoded path, the
  query (ordered by name), the canonical `If-Match` and the body in canonical
  JSON (members sorted, numbers by exact value, no whitespace). The body is
  captured for every command wherever its controller is declared; a body that
  was sent but not read as the command's `@RequestBody` fails the request
  (500) rather than hash as empty.
- Every handler that can answer under `/api/v2` lives in `web.v2`
  (`V2HandlerPlacementIT`), so the ArchUnit rules and the contract test see
  all of them.
- The record keeps the caller's user id, the key, that hash and the outcome
  (resource type, id, version, response status) for 72 hours; expired keys
  count as absent and are purged hourly. No value of the request or the
  response is stored.
- A response, first or replayed, comes from the method's `OutcomeRenderer`,
  which renders the resource at the outcome's version from its history. For
  submissions that history is the revision journal (step C) through the
  submission normalizer; until a module has a history, it cannot offer
  replayable commands.
- A dry run is the same request with `dryRun: true`: it sends the same
  headers (the key and `If-Match`), which are checked the same way, including
  412 when stale, but it never runs through `execute`, so it claims nothing and
  writes nothing; the change itself may follow with the same key.
- A create of a resource with no version yet (a note, a grant) takes
  `CreateRequest`: an `Idempotency-Key` and no `If-Match` (one sent is 400
  `invalid-if-match`). `IdempotentCommands.create` claims and replays like a
  change and answers 201 with `Location` and the first version's `ETag`.
- Command bodies are at most 1 MB (413 `payload-too-large`).

## Activation Switches

Deploying a release and turning on what it brings are separate steps
(`production-boundaries.md`). `config.activation`:

- A module declares each feature it ships off as an `ActivationSwitch` bean:
  a kebab-case name from its glossary, what it turns on, and the switches it
  requires. E0 declares none: the later steps declare theirs with the
  glossary's words, and the activation order the design note sets becomes
  their requirements, so a feature cannot be turned on before what makes it
  safe. The first declared is submission review's `conflict-detection`
  (`ConflictDetectionSwitch`, step S2), which the upload routes read; it
  requires none.
- `ActivationSwitches` reads `datarun.activation.<name>` once at startup.
  Nothing is on by default, anywhere. A switch whose requirement is off stays
  off (ERROR line); a value other than true or false, or a setting no switch
  of the build declares, is a WARN line, never a failed start, so phones keep
  their service and a rollback to an older build still starts. The states are
  logged at startup.
- In production a switch is set in the host environment, for example
  `DATARUN_ACTIVATION_SOME_FEATURE=true` passed by `compose.yml`, and the
  container recreated; setting it back to false is the rollback. Neither
  compose file passes a `DATARUN_ACTIVATION_*` variable yet: turning a switch
  on adds it to the service's `environment` list.
- A v2 handler calls `requireMethod` for a whole method behind a switch (422
  `method-not-enabled`) or `requireOperation` for one kind of operation in a
  request (422 `operation-not-enabled`); either names its switch.

## The Published Contract

- The v2 OpenAPI document is group `v2` (`web.v2.config.V2OpenApiConfiguration`),
  served at `/v3/api-docs/v2` (admin only, where the api-docs profile is on,
  as in production) and checked in as `docs/api/openapi-v2.json`.
- It documents the conventions once: the bearer scheme required everywhere;
  the `Id`, `Problem` and `ProblemError` schemas; a `default` problem response
  on every operation; `Idempotency-Key`, `If-Match`, 412 and 428 on every
  command; `limit` and `cursor` on every list. Problem codes are listed as
  `x-extensible-enum`, an open list, so generated clients do not reject a new
  code. Every response enumeration is marked open the same way: one only
  responses use is published as `x-extensible-enum`; one a request uses too
  keeps `enum` (a request may only send listed values) and its description
  says responses may add values. An enumeration once published open stays
  open: the check calls a response's open list turning closed a break, so a
  request that needs a closed list gets a schema of its own.
- `V2OpenApiContractTest` is a unit test, so `scripts/release/verify.sh`
  (`./mvnw test`) runs it; the repository has no CI. It generates the
  document from the v2 controllers in a web slice without a database and
  compares it with the snapshot (`OpenApiCompatibility`): a breaking change
  fails, and any other difference fails until the snapshot is updated, so the
  snapshot is always the contract.
- Additive inside v2: new operations, optional parameters, optional request
  members, new response members, new enumeration values and codes. Breaking:
  anything removed or renamed, a parameter or member newly required, a type,
  format or limit that narrows what a client may send or widens what it may
  receive, a new success status, a security change, and any schema change the
  comparison does not model.
- To publish an additive change: `./mvnw test -Dtest=V2OpenApiContractTest
  -Dopenapi.v2.update=true`, then commit the snapshot with the change. A
  breaking change also needs `-Dopenapi.v2.acceptBreaking=true` and a
  reviewed decision; v2 takes none in ordinary work.
- A v2 controller with collaborators adds them to that test as
  `@MockitoBean`s. `V2OpenApiLiveIT` holds the running application to the
  same snapshot, test fixtures hidden (`@Hidden`).
- The document is OpenAPI 3.1 (springdoc 2.8's default), so a type is
  compared as a set (`["string", "null"]`). oneOf and anyOf variants are
  matched by the reference they name (else title, else position) and followed
  into their schemas, discriminator mappings are compared, and map value
  schemas (`additionalProperties`, as for values keyed by field id) are
  compared by direction like any other. Schemas are compared through the
  operations that use them; E0 publishes no operation yet, so its check bites
  from the first v2 route on (the test proves it fails on a deliberate break).
- The same test checks the conventions that show in the document
  (`V2Conventions`): paths under `/api/v2` in kebab-case with `{camelCase}`
  variables and `:verb` actions; GET, POST and DELETE only; camelCase members
  and parameters; UPPER_SNAKE enumeration values; no bare `status` outside the
  problem; ids are the `Id` schema and never a `uid`; no `format: uuid` but
  the `Idempotency-Key` header; a default problem response and the global
  bearer requirement.
- `V2ArchitectureTest`: nothing in `web.v2` touches a JPA entity (domain work
  for v2 lives outside it and hands it its own types), and nothing in
  `web.v2` uses the v1 resources in `web.rest`.
