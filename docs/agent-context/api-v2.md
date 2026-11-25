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
