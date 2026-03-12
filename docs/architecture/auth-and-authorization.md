# Authentication & Authorization

> **Status:** Draft — Living Document
> **Ground Truth:** The `datarunapi` Java codebase is the sole operational system.

## Overview

Authentication is the number one reason "decoupled" systems accidentally become tightly coupled. This document enforces a strict **identity vs. authorization split** to keep boundaries clean.

> [!IMPORTANT]
> **Identity** (who are you?) is owned by **DatarunAPI**. **Authorization** (what can you do in a specific domain?) is owned by each downstream consumer. These concerns must never be conflated. See [ADR-008](../adrs/008-auth-phased-strategy.md).

---

## 1. Authentication (Identity: "Who are you?")

### Phase 1: DatarunAPI as Identity Provider (Current)

DatarunAPI is the **single identity provider** for all systems:

| Channel | Flow |
|---|---|
| **Mobile App** | User logs in → DatarunAPI issues RS256 JWT → app uses token for API calls |
| **Web Frontend** | User logs in via DatarunAPI → DatarunAPI issues RS256 JWT → SPA uses token |
| **Service-to-Service** | Service account authenticates to DatarunAPI → receives JWT |

**How token validation works:**
1. DatarunAPI signs all JWTs with RS256 (asymmetric).
2. DatarunAPI exposes the public key at `/.well-known/jwks.json`.
3. Any consumer validates tokens using this public key. **No shared secrets.**
4. Adding a new consumer requires only configuring a JWT decoder with DatarunAPI's JWKS URI.

**SSO is automatic:** A user authenticates once. That JWT is valid against any system that validates via the same JWKS endpoint.

### Phase 2: Federated Identity (Future)

When user management complexity demands it, deploy Keycloak (or equivalent). DatarunAPI and all consumers become **Relying Parties** of the IdP. The identity/authorization split below remains unchanged — only the token source changes.

---

## 2. Identity vs. Authorization Split

> [!CAUTION]
> DatarunAPI's JWT must **never** contain domain-specific claims (e.g., `allowed_nodes`, `ledger_supervisor`, `submit_adapter_payload`). Domain-specific vocabulary belongs in each consumer's own authorization layer.

### What DatarunAPI's JWT Contains (Identity Only)

```json
{
  "sub": "user_uuid_5678",
  "name": "Jane Doe",
  "roles": ["DATA_COLLECTOR", "SUPERVISOR"]
}
```

Only generic, domain-agnostic claims. DatarunAPI doesn't know what a "ledger" or "node" or "case" is.

### What Downstream Systems Add (Authorization)

Each consuming system maintains its **own authorization layer** in its own database, keyed by the `sub` (user ID) from the JWT. This keeps the identity provider clean and allows each domain to define its own access semantics independently.

**The general pattern:**
1. **Validate JWT** signature via DatarunAPI's JWKS → proves identity.
2. **Look up `sub`** in the consumer's own permissions store → resolves domain-specific authorization.
3. **Build a domain-specific context** from both sources → inject into domain services.

> [!NOTE]
> This pattern is **phase-agnostic**. Whether the JWT comes from DatarunAPI (Phase 1) or Keycloak (Phase 2), only the token validation step changes. Authorization lookup and domain services remain untouched.

---

## 3. Design Principles

1. **Identity = DatarunAPI.** Single sign-on across all services via JWKS.
2. **Authorization = each domain.** Roles, access scopes, and permissions live in each consumer's own DB.
3. **JWT contains identity only.** No domain-specific vocabulary in the token.
4. **Domain services never import JWT libraries.** They receive a context object and nothing else.
5. **Phase 2 migration is clean.** Keycloak replaces DatarunAPI as identity provider. Authorization layers stay unchanged.

---

## 4. Future: Permission Verbs (Deferred)

The current authorization model checks **role names** (nouns). A more extensible model checks **permissions** (verbs): e.g., `has_permission("approve_command")` instead of `require_role("supervisor")`.

### Why Deferred

At current scale (single builder, single deployment), role-based checks are sufficient. The permission-verb model adds value when: (a) multiple teams need fine-grained access control, or (b) role definitions need to vary per deployment without code changes.

---

## Related Docs

- [ADR-008: Auth Phased Strategy](../adrs/008-auth-phased-strategy.md)
- [Context Map](context-map.md)
- [Integration Contract — DatarunAPI](integration-contract-datarunapi.md)
